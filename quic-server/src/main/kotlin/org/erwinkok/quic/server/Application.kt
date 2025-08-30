package org.erwinkok.quic.server

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.network.sockets.InetSocketAddress
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.erwinkok.quic.common.quiche.Quiche

private val logger = KotlinLogging.logger {}

fun main() {
    runBlocking {
        logger.info { "Starting QuicServer..." }

        Quiche.quiche_version()

        val quicConfiguration = QuicheServerQuicConfiguration()

        val scope = CoroutineScope(SupervisorJob() + exceptionHandler + Dispatchers.Default)
        val localInetSocket = InetSocketAddress("localhost", 8484)
        val connection = QuicheServerConnection.create(scope, localInetSocket, quicConfiguration)
        connection.close()
        connection.awaitClosed()
    }
}

private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
    val cause = throwable.cause ?: throwable
    val message = cause.message ?: cause.toString()
    logger.error { "Error: $message" }
}
