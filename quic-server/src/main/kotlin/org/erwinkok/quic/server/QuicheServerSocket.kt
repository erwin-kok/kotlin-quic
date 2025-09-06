package org.erwinkok.quic.server

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.toJavaAddress
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.remaining
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import org.erwinkok.quic.common.AwaitableClosable
import org.erwinkok.quic.common.QuicConfiguration
import org.erwinkok.quic.common.QuicHeaderOld
import org.erwinkok.quic.common.QuicHeader
import org.erwinkok.quic.common.QuicheConnectionId
import org.erwinkok.quic.common.QuicheConstants.QUICHE_MAX_CONN_ID_LEN
import org.erwinkok.quic.common.QuicheConstants.QUICHE_PROTOCOL_VERSION
import org.erwinkok.quic.common.quiche.Quiche
import org.erwinkok.quic.common.quiche.QuicheRecvInfo
import org.erwinkok.quic.common.quiche.convert
import org.erwinkok.quic.common.toMemorySegment
import org.erwinkok.quic.common.toPacket
import java.io.IOException
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator
import java.lang.foreign.ValueLayout.JAVA_LONG
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
    private val connections = ConcurrentHashMap<QuicheConnectionId, QuicheServerConnection>()
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

                Arena.ofShared().use { arena ->
                    val byteArray = datagram.packet.readByteArray()
                    val inputSegment = arena.allocate(byteArray.size.toLong())
                    inputSegment.copyFrom(MemorySegment.ofArray(byteArray))
                    val quicHeader = QuicHeaderOld.parse(inputSegment)
                    logger.debug { "quic header: $quicHeader" }

                    val qh = QuicHeader.parse(
                        buildPacket {
                            write(byteArray)
                        },
                    )

                    val s = qh.token.bytes?.size ?: 0
                    if (s > 0) {
                        println()
                    }

                    val connectionId = quicHeader.dcid
                    var connection = connections[connectionId]
                    if (connection == null) {
                        connection = negotiate(remoteAddress, quicHeader)
                    }
                    if (connection != null) {
                        val remaining = inputSegment.asSlice(quicHeader.size)
                        val recvInfo = QuicheRecvInfo.allocate(arena)
                        val local = inetLocalAddress.toJavaAddress()
                        val peer = remoteAddress.toJavaAddress()
                        QuicheRecvInfo.setSocketAddress(recvInfo, local, peer, arena)
                        val received = Quiche.quiche_conn_recv(
                            connection.quicheConnection,
                            remaining,
                            remaining.byteSize(),
                            recvInfo,
                        )
                        if (received < 0) {
                            logger.error { "failed to process packet: $received" }
                        } else if (Quiche.quiche_conn_is_established(connection.quicheConnection)) {
                            val quiche_stream_iter = Quiche.quiche_conn_readable(connection.quicheConnection)
                            val streamIdSegment = arena.allocate(JAVA_LONG)
                            while (Quiche.quiche_stream_iter_next(quiche_stream_iter, streamIdSegment)) {
                                val streamId = streamIdSegment.get(JAVA_LONG, 0L)
                                logger.info { "stream $streamId is readable" }

                            }
                        }
                    }
                }
            }
        }.invokeOnCompletion {
            socket.close()
        }
    }

    override fun close() {
        context.complete()
    }

    private suspend fun negotiate(remoteAddress: InetSocketAddress, quicHeader: QuicHeaderOld): QuicheServerConnection? {
        if (!Quiche.quiche_version_is_supported(quicHeader.version)) {
            logger.debug { "version negotiation" }
            versionNegotiation(remoteAddress, quicHeader)
            return null
        }
        if (quicHeader.tokenLength == 0) {
            logger.debug { "stateless retry" }
            statelessRetry(quicHeader, remoteAddress)
            return null
        }
        val odcid = validateToken(quicHeader.token, remoteAddress)
        if (odcid == null) {
            logger.error { "invalid address validation token" }
            return null
        }
        return createConnection(quicHeader.dcid.connectionId, odcid, inetLocalAddress, remoteAddress)
    }

    private fun createConnection(
        scidBytes: ByteArray,
        odcidBytes: ByteArray,
        localAddress: InetSocketAddress,
        remoteAddress: InetSocketAddress,
    ): QuicheServerConnection? {
        val arena = Arena.ofConfined()
        try {
            Arena.ofShared().use { tempArena ->
                if (scidBytes.size != QUICHE_MAX_CONN_ID_LEN.toInt()) {
                    logger.error { "failed, scid length too short" }
                    arena.close()
                    return null
                }
                val scidSegment = scidBytes.toMemorySegment(arena)
                val odcidSegment = odcidBytes.toMemorySegment(tempArena)
                val localAddressSegment = localAddress.toJavaAddress().convert(tempArena)
                val remoteAddressSegment = remoteAddress.toJavaAddress().convert(tempArena)
                val libQuicheConfig = buildConfig(quicConfiguration, tempArena)
                val quicheConnection = Quiche.quiche_accept(
                    scidSegment,
                    scidSegment.byteSize(),
                    odcidSegment,
                    odcidSegment.byteSize(),
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
                return QuicheServerConnection(arena, scidSegment, quicheConnection)
            }
        } catch (e: Exception) {
            arena.close()
            throw e
        }
    }

    private suspend fun versionNegotiation(remoteAddress: InetSocketAddress, quicHeader: QuicHeaderOld) {
        Arena.ofShared().use { arena ->
            val out = arena.allocate(MAX_DATAGRAM_SIZE.toLong())
            val length = Quiche.quiche_negotiate_version(
                quicHeader.scidSegment,
                quicHeader.scidLength.toLong(),
                quicHeader.dcidSegment,
                quicHeader.dcidLength.toLong(),
                out,
                MAX_DATAGRAM_SIZE.toLong(),
            )
            val packet = out.toPacket(length.toInt())
            sendChannel.send(Datagram(packet, remoteAddress))
        }
    }

    private suspend fun statelessRetry(quicHeader: QuicHeaderOld, remoteAddress: InetSocketAddress) {
        val token = mintToken(quicHeader.dcid, remoteAddress)
        val newCid = ByteArray(QUICHE_MAX_CONN_ID_LEN.toInt())
        Random.nextBytes(newCid)
        Arena.ofShared().use { arena ->
            val newCidSegment = newCid.toMemorySegment(arena)
            val tokenSegment = token.toMemorySegment(arena)
            val out = arena.allocate(MAX_DATAGRAM_SIZE.toLong())
            val length = Quiche.quiche_retry(
                quicHeader.scidSegment,
                quicHeader.scidLength.toLong(),
                quicHeader.dcidSegment,
                quicHeader.dcidLength.toLong(),
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

    private fun mintToken(dcid: QuicheConnectionId, remoteAddress: InetSocketAddress): ByteArray {
        val name = "kotlin-quic".toByteArray(StandardCharsets.US_ASCII)
        val address = remoteAddress.resolveAddress() ?: byteArrayOf()
        val port = remoteAddress.port
        val connectionId = dcid.connectionId
        val token = ByteBuffer.allocate(name.size + address.size + 4 + connectionId.size)
        token.put(name)
        token.put(address)
        token.putInt(port)
        token.put(connectionId)
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
