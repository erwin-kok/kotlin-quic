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
import kotlinx.coroutines.launch
import org.erwinkok.quic.common.AwaitableClosable

private val logger = KotlinLogging.logger {}

class QuicheServerConnection(
    scope: CoroutineScope,
    private val inetLocalAddress: InetSocketAddress,
    private val quicConfiguration: QuicheServerQuicConfiguration,
    private val socket: BoundDatagramSocket,
) : AwaitableClosable {
    private val context = Job(scope.coroutineContext[Job])

    override val jobContext: Job get() = context

    val localPort: Int
        get() = inetLocalAddress.port

    val localHostname: String
        get() = inetLocalAddress.hostname

    init {
        logger.info { "Initializing QuicheServerConnection, listening on $inetLocalAddress" }
        scope.launch(context + CoroutineName("quiche-server-connection-$inetLocalAddress")) {
        }
    }

    override fun close() {
        context.complete()
    }

    companion object Companion {
        suspend fun create(
            scope: CoroutineScope,
            inetLocalAddress: InetSocketAddress,
            quicConfiguration: QuicheServerQuicConfiguration,
        ): QuicheServerConnection {
            val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).udp().bind(inetLocalAddress)
            return QuicheServerConnection(scope, inetLocalAddress, quicConfiguration, socket)
        }
    }
}
