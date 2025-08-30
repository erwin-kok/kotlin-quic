package org.erwinkok.quic.server

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.erwinkok.quic.common.quiche.Quiche

private val logger = KotlinLogging.logger {}

fun main() {
    runBlocking {
        logger.info { "Starting QuicServer..." }
        Quiche.quiche_version()
    }
}
