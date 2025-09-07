package org.erwinkok.quic.server

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.toJavaAddress
import io.ktor.utils.io.core.remaining
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.erwinkok.quic.common.AwaitableClosable
import org.erwinkok.quic.common.BinaryDataHolder
import org.erwinkok.quic.common.QuicConfiguration
import org.erwinkok.quic.common.QuicHeader
import org.erwinkok.quic.common.QuicheConstants.QUICHE_MAX_CONN_ID_LEN
import org.erwinkok.quic.common.QuicheConstants.QUICHE_PROTOCOL_VERSION
import org.erwinkok.quic.common.quiche.Quiche
import org.erwinkok.quic.common.quiche.QuicheError
import org.erwinkok.quic.common.quiche.convert
import org.erwinkok.quic.common.toMemorySegment
import org.erwinkok.quic.common.toPacket
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

class QuicheServerSocket(
    scope: CoroutineScope,
    private val inetLocalAddress: InetSocketAddress,
    private val quicConfiguration: QuicConfiguration,
    private val socket: BoundDatagramSocket,
) : AwaitableClosable {
    private val context = Job(scope.coroutineContext[Job])
    private val connections = ConcurrentHashMap<BinaryDataHolder, QuicheServerConnection>()
    private val receiveChannel = socket.incoming
    private val sendChannel = socket.outgoing

    override val jobContext: Job get() = context

    val localPort: Int
        get() = inetLocalAddress.port

    val localHostname: String
        get() = inetLocalAddress.hostname

    init {
        logger.info { "QuicheServerSocket listening on $inetLocalAddress" }
        scope.launch(context + CoroutineName("quiche-server-socket-$inetLocalAddress")) {
            while (isActive) {
                val datagram = receiveChannel.receive()
                val remoteAddress = datagram.address as? InetSocketAddress
                if (remoteAddress == null) {
                    logger.error { "unable to get remote address for datagram: $datagram" }
                    continue
                }

                logger.debug { "message received from $remoteAddress (${datagram.packet.remaining} bytes)" }

                val inputSource = datagram.packet
                val quicHeader = QuicHeader.parse(inputSource)
                logger.debug { "quic header: $quicHeader" }

                val connectionId = quicHeader.dcid
                var connection = connections[connectionId]
                if (connection == null) {
                    connection = negotiate(remoteAddress, quicHeader)
                }
                if (connection != null) {
                    connection.feedCipherBytes(inputSource, inetLocalAddress, remoteAddress)
                }
            }
        }.invokeOnCompletion {
            socket.close()
        }
    }

    override fun close() {
        context.complete()
    }

    private suspend fun negotiate(remoteAddress: InetSocketAddress, quicHeader: QuicHeader): QuicheServerConnection? {
        if (!Quiche.quiche_version_is_supported(quicHeader.version)) {
            logger.debug { "version negotiation" }
            versionNegotiation(remoteAddress, quicHeader)
            return null
        }
        val tokenBytes = quicHeader.token.bytes
        if (tokenBytes.isEmpty()) {
            logger.debug { "stateless retry" }
            statelessRetry(quicHeader, remoteAddress)
            return null
        }
        val odcid = validateToken(tokenBytes, remoteAddress)
        if (odcid == null) {
            logger.error { "invalid address validation token" }
            return null
        }
        return createConnection(quicHeader.dcid, odcid, inetLocalAddress, remoteAddress)
    }

    private fun createConnection(
        scid: BinaryDataHolder,
        odcidBytes: ByteArray,
        localAddress: InetSocketAddress,
        remoteAddress: InetSocketAddress,
    ): QuicheServerConnection? {
        val arena = Arena.ofConfined()
        try {
            Arena.ofShared().use { tempArena ->
                if (scid.size != QUICHE_MAX_CONN_ID_LEN.toInt()) {
                    logger.error { "failed, scid length too short" }
                    arena.close()
                    return null
                }
                val odcid = BinaryDataHolder.of(odcidBytes, tempArena)
                val localAddressSegment = localAddress.toJavaAddress().convert(tempArena)
                val remoteAddressSegment = remoteAddress.toJavaAddress().convert(tempArena)
                val libQuicheConfig = buildConfig(quicConfiguration, tempArena)
                val quicheConnection = Quiche.quiche_accept(
                    scid.segment,
                    scid.segment.byteSize(),
                    odcid.segment,
                    odcid.segment.byteSize(),
                    localAddressSegment,
                    localAddressSegment.byteSize().toInt(),
                    remoteAddressSegment,
                    remoteAddressSegment.byteSize().toInt(),
                    libQuicheConfig,
                )
                if (quicheConnection == null) {
                    logger.error { "failed, could not create quiche connection" }
                    arena.close()
                    return null
                }
                return QuicheServerConnection(arena, quicheConnection)
            }
        } catch (e: Exception) {
            arena.close()
            throw e
        }
    }

    private suspend fun versionNegotiation(remoteAddress: InetSocketAddress, quicHeader: QuicHeader) {
        Arena.ofShared().use { arena ->
            val out = arena.allocate(MAX_DATAGRAM_SIZE.toLong())
            val length = Quiche.quiche_negotiate_version(
                quicHeader.scid.segment,
                quicHeader.scid.size.toLong(),
                quicHeader.dcid.segment,
                quicHeader.dcid.size.toLong(),
                out,
                MAX_DATAGRAM_SIZE.toLong(),
            )
            val packet = out.toPacket(length.toInt())
            sendChannel.send(Datagram(packet, remoteAddress))
        }
    }

    private suspend fun statelessRetry(quicHeader: QuicHeader, remoteAddress: InetSocketAddress) {
        val token = mintToken(quicHeader.dcid.bytes, remoteAddress)
        val newCid = ByteArray(QUICHE_MAX_CONN_ID_LEN.toInt())
        Random.nextBytes(newCid)
        Arena.ofShared().use { arena ->
            val newCidSegment = newCid.toMemorySegment(arena)
            val tokenSegment = token.toMemorySegment(arena)
            val out = arena.allocate(MAX_DATAGRAM_SIZE.toLong())
            val length = Quiche.quiche_retry(
                quicHeader.scid.segment,
                quicHeader.scid.size.toLong(),
                quicHeader.dcid.segment,
                quicHeader.dcid.size.toLong(),
                newCidSegment,
                QUICHE_MAX_CONN_ID_LEN,
                tokenSegment,
                token.size.toLong(),
                quicHeader.version,
                out,
                MAX_DATAGRAM_SIZE.toLong(),
            )
            val packet = out.toPacket(length.toInt())
            sendChannel.send(Datagram(packet, remoteAddress))
        }
    }

    private fun mintToken(dcidBytes: ByteArray, remoteAddress: InetSocketAddress): ByteArray {
        val name = "kotlin-quic".toByteArray(StandardCharsets.US_ASCII)
        val address = remoteAddress.resolveAddress() ?: byteArrayOf()
        val port = remoteAddress.port
        val token = ByteBuffer.allocate(name.size + address.size + 4 + dcidBytes.size)
        token.put(name)
        token.put(address)
        token.putInt(port)
        token.put(dcidBytes)
        return token.array()
    }

    private fun validateToken(token: ByteArray, remoteAddress: InetSocketAddress): ByteArray? {
        val name = "kotlin-quic".toByteArray(StandardCharsets.US_ASCII)
        val buffer = ByteBuffer.wrap(token)
        if (!compareSubToken(buffer, name)) {
            return null
        }
        val address = remoteAddress.resolveAddress() ?: byteArrayOf()
        if (!compareSubToken(buffer, address)) {
            return null
        }
        if (buffer.remaining() < 4) {
            return null
        }
        val port = buffer.getInt()
        if (port != remoteAddress.port) {
            return null
        }
        val dcid = ByteArray(buffer.remaining())
        buffer.get(dcid)
        return dcid
    }

    private fun compareSubToken(buffer: ByteBuffer, expect: ByteArray): Boolean {
        if (buffer.remaining() < expect.size) {
            return false
        }
        val actual = ByteArray(expect.size)
        buffer.get(actual)
        return actual.contentEquals(expect)
    }

    private fun buildConfig(config: QuicConfiguration, allocator: SegmentAllocator): MemorySegment {
        val quicheConfig = Quiche.quiche_config_new(QUICHE_PROTOCOL_VERSION)
        if (quicheConfig == null) {
            throw IOException("Failed to create quiche config")
        }
        val verifyPeer = config.verifyPeer
        if (verifyPeer != null) {
            Quiche.quiche_config_verify_peer(quicheConfig, verifyPeer)
        }
        val trustedCertsPemPath = config.trustedCertificatesPemPath
        if (trustedCertsPemPath != null) {
            val rc = Quiche.quiche_config_load_verify_locations_from_file(quicheConfig, allocator.allocateUtf8String(trustedCertsPemPath.toString()))
            if (rc < 0) {
                throw IOException("Error loading trusted certificates file " + trustedCertsPemPath + " : " + QuicheError.errorString(rc))
            }
        }
        val certChainPemPath = config.certificateChainPemPath
        if (certChainPemPath != null) {
            val rc = Quiche.quiche_config_load_cert_chain_from_pem_file(quicheConfig, allocator.allocateUtf8String(certChainPemPath.toString()))
            if (rc < 0) {
                throw IOException("Error loading certificate chain file " + certChainPemPath + " : " + QuicheError.errorString(rc))
            }
        }
        val privKeyPemPath = config.privateKeyPemPath
        if (privKeyPemPath != null) {
            val rc = Quiche.quiche_config_load_priv_key_from_pem_file(quicheConfig, allocator.allocateUtf8String(privKeyPemPath.toString()))
            if (rc < 0) {
                throw IOException("Error loading private key file " + privKeyPemPath + " : " + QuicheError.errorString(rc))
            }
        }
        val applicationProtos = config.applicationProtocols
        if (!applicationProtos.isNullOrEmpty()) {
            val baos = ByteArrayOutputStream()
            for (proto in applicationProtos) {
                val bytes = proto.toByteArray(StandardCharsets.UTF_8)
                baos.write(bytes.size)
                baos.write(bytes)
            }
            val bytes = baos.toByteArray()
            val segment = allocator.allocate(bytes.size.toLong())
            segment.asByteBuffer().put(bytes)
            val rc = Quiche.quiche_config_set_application_protos(quicheConfig, segment, segment.byteSize())
            if (rc < 0) {
                throw IOException("Error setting application protocols : " + QuicheError.errorString(rc))
            }
        }
        val cc = config.congestionControlAlgorithm
        if (cc != null) {
            Quiche.quiche_config_set_cc_algorithm(quicheConfig, cc.value)
        }
        val maxIdleTimeout = config.maxIdleTimeout
        if (maxIdleTimeout != null) {
            Quiche.quiche_config_set_max_idle_timeout(quicheConfig, maxIdleTimeout)
        }
        val initialMaxData = config.initialMaxData
        if (initialMaxData != null) {
            Quiche.quiche_config_set_initial_max_data(quicheConfig, initialMaxData)
        }
        val initialMaxStreamDataBidiLocal = config.initialMaxStreamDataBidiLocal
        if (initialMaxStreamDataBidiLocal != null) {
            Quiche.quiche_config_set_initial_max_stream_data_bidi_local(quicheConfig, initialMaxStreamDataBidiLocal.toLong())
        }
        val initialMaxStreamDataBidiRemote = config.initialMaxStreamDataBidiRemote
        if (initialMaxStreamDataBidiRemote != null) {
            Quiche.quiche_config_set_initial_max_stream_data_bidi_remote(quicheConfig, initialMaxStreamDataBidiRemote.toLong())
        }
        val initialMaxStreamDataUni = config.initialMaxStreamDataUni
        if (initialMaxStreamDataUni != null) {
            Quiche.quiche_config_set_initial_max_stream_data_uni(quicheConfig, initialMaxStreamDataUni)
        }
        val initialMaxStreamsBidi = config.initialMaxStreamsBidi
        if (initialMaxStreamsBidi != null) {
            Quiche.quiche_config_set_initial_max_streams_bidi(quicheConfig, initialMaxStreamsBidi)
        }
        val initialMaxStreamsUni = config.initialMaxStreamsUni
        if (initialMaxStreamsUni != null) {
            Quiche.quiche_config_set_initial_max_streams_uni(quicheConfig, initialMaxStreamsUni)
        }
        val disableActiveMigration: Boolean? = config.disableActiveMigration
        if (disableActiveMigration != null) {
            Quiche.quiche_config_set_disable_active_migration(quicheConfig, disableActiveMigration)
        }
        val maxConnectionWindow = config.maxConnectionWindow
        if (maxConnectionWindow != null) {
            Quiche.quiche_config_set_max_connection_window(quicheConfig, maxConnectionWindow)
        }
        val maxStreamWindow = config.maxStreamWindow
        if (maxStreamWindow != null) {
            Quiche.quiche_config_set_max_stream_window(quicheConfig, maxStreamWindow)
        }
        val activeConnectionIdLimit = config.activeConnectionIdLimit
        if (activeConnectionIdLimit != null) {
            Quiche.quiche_config_set_active_connection_id_limit(quicheConfig, activeConnectionIdLimit)
        }
        val maxReceiveUdpPayloadSize = config.maxReceiveUdpPayloadSize
        if (maxReceiveUdpPayloadSize != null) {
            Quiche.quiche_config_set_max_recv_udp_payload_size(quicheConfig, maxReceiveUdpPayloadSize)
        }
        val maxSendUdpPayloadSize = config.maxSendUdpPayloadSize
        if (maxSendUdpPayloadSize != null) {
            Quiche.quiche_config_set_max_send_udp_payload_size(quicheConfig, maxSendUdpPayloadSize)
        }
        return quicheConfig
    }

    companion object Companion {
        suspend fun create(
            scope: CoroutineScope,
            inetLocalAddress: InetSocketAddress,
            quicConfiguration: QuicConfiguration,
        ): QuicheServerSocket {
            val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).udp().bind(inetLocalAddress)
            return QuicheServerSocket(scope, inetLocalAddress, quicConfiguration, socket)
        }
    }
}
