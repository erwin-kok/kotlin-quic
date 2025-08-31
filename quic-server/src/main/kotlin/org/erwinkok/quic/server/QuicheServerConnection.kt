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
import kotlinx.io.readByteArray
import org.erwinkok.quic.common.AwaitableClosable
import org.erwinkok.quic.common.QuicConfiguration
import org.erwinkok.quic.common.QuicHeader
import java.nio.ByteBuffer

private val logger = KotlinLogging.logger {}

class QuicheServerConnection(
    scope: CoroutineScope,
    private val inetLocalAddress: InetSocketAddress,
    private val quicConfiguration: QuicConfiguration,
    private val socket: BoundDatagramSocket,
) : AwaitableClosable {
    private val context = Job(scope.coroutineContext[Job])
    private val receiveChannel = socket.incoming
    private val sendChannel = socket.outgoing

    override val jobContext: Job get() = context

    val localPort: Int
        get() = inetLocalAddress.port

    val localHostname: String
        get() = inetLocalAddress.hostname

    init {
        logger.info { "QuicheServerConnection listening on $inetLocalAddress" }
        scope.launch(context + CoroutineName("quiche-server-connection-$inetLocalAddress")) {
            while (isActive) {
                val datagram = receiveChannel.receive()
                val remoteAddress = datagram.address as? InetSocketAddress
                if (remoteAddress == null) {
                    logger.error { "unable to get remote address for datagram: $datagram" }
                    continue
                }

                logger.debug { "message received from $remoteAddress" }
                val packetRead = ByteBuffer.wrap(datagram.packet.readByteArray())
                val quicHeader = QuicHeader.parse(packetRead)
            }
        }.invokeOnCompletion {
            socket.close()
        }
    }

    override fun close() {
        context.complete()
    }

    companion object Companion {
        suspend fun create(
            scope: CoroutineScope,
            inetLocalAddress: InetSocketAddress,
            quicConfiguration: QuicConfiguration,
        ): QuicheServerConnection {
            val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).udp().bind(inetLocalAddress)
            return QuicheServerConnection(scope, inetLocalAddress, quicConfiguration, socket)
        }
    }
}
