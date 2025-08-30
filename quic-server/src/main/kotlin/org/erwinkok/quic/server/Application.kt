package org.erwinkok.quic.server

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}

fun main() {
    runBlocking {
        logger.info { "Starting QuicServer..." }
    }
}

private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
    val cause = throwable.cause ?: throwable
    val message = cause.message ?: cause.toString()
    logger.error { "Error: $message" }
}
