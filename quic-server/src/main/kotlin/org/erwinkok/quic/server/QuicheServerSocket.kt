package org.erwinkok.quic.server

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.erwinkok.quic.common.AwaitableClosable
import org.erwinkok.quic.common.QuicConfiguration
import org.erwinkok.quic.common.QuicHeader
import org.erwinkok.quic.common.QuicheConnectionId
import org.erwinkok.quic.common.quiche.Quiche
import java.util.concurrent.ConcurrentHashMap

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

                logger.debug { "message received from $remoteAddress" }
                val quicHeader = QuicHeader.parse(datagram.packet)
                val connectionId = quicHeader.dcid
                var connection = connections[connectionId]
                if (connection == null) {
                    negotiate(quicHeader)
                }
            }
        }.invokeOnCompletion {
            socket.close()
        }
    }

    override fun close() {
        context.complete()
    }

    private fun negotiate(quicHeader: QuicHeader) {
        if (!Quiche.quiche_version_is_supported(quicHeader.version)) {
            logger.debug { "version negotiation" }
        }
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
