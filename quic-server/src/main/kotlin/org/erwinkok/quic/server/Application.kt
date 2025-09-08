package org.erwinkok.quic.server

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.network.sockets.InetSocketAddress
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.erwinkok.quic.common.CongestionControlAlgorithm
import org.erwinkok.quic.common.QuicConfiguration
import kotlin.io.path.Path

private val logger = KotlinLogging.logger {}

const val MAX_DATAGRAM_SIZE = 1350

fun main() {
    runBlocking {
        logger.info { "Starting QuicServer..." }
        val quicConfiguration = QuicConfiguration(
            verifyPeer = false,
            certificateChainPemPath = Path("./certs/cert.crt"),
            privateKeyPemPath = Path("./certs/cert.key"),
            applicationProtocols = listOf("hq-interop", "hq-29", "hq-28", "hq-27", "http/0.9"),
            maxIdleTimeout = 5000,
            maxReceiveUdpPayloadSize = MAX_DATAGRAM_SIZE.toLong(),
            maxSendUdpPayloadSize = MAX_DATAGRAM_SIZE.toLong(),
            initialMaxData = 10000000,
            initialMaxStreamDataBidiLocal = 1000000,
            initialMaxStreamDataBidiRemote = 1000000,
            initialMaxStreamsBidi = 100,
            congestionControlAlgorithm = CongestionControlAlgorithm.QUICHE_CC_RENO,
        )
        val scope = CoroutineScope(SupervisorJob() + exceptionHandler + Dispatchers.Default)
        val localInetSocket = InetSocketAddress("localhost", 8484)
        val connection = QuicheServerSocket.create(scope, localInetSocket, quicConfiguration)
        connection.close()
        connection.awaitClosed()
    }
}

private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
    val cause = throwable.cause ?: throwable
    val message = cause.message ?: cause.toString()
    logger.error { "Error: $message" }
}
