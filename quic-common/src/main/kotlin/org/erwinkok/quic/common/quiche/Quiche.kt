package org.erwinkok.quic.common.quiche

import io.github.oshai.kotlinlogging.KotlinLogging
import org.erwinkok.quic.common.quiche.NativeHelper.C_POINTER
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment

private val logger = KotlinLogging.logger {}

@Suppress("ktlint:standard:function-naming")
object Quiche {
    private const val EXPECTED_QUICHE_VERSION = "0.24.4"

    init {
        val quicheVersion = quiche_version().getUtf8String(0L)
        require(quicheVersion == EXPECTED_QUICHE_VERSION) {
            "Unexpected Native Quiche version: $quicheVersion, expected ${EXPECTED_QUICHE_VERSION}"
        }
        if (logger.isDebugEnabled()) {
            logger.debug { "Loaded Native Quiche version: $quicheVersion" }
        }
    }

    fun quiche_version(): MemorySegment {
        return DowncallHandles.quiche_version.invokeExact() as MemorySegment
    }

    private object DowncallHandles {
        val quiche_version = NativeHelper.downcallHandle(
            "quiche_version",
            FunctionDescriptor.of(C_POINTER),
        )
    }
}
