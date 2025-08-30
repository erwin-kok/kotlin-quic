package org.erwinkok.quic.common.quiche

import io.github.oshai.kotlinlogging.KotlinLogging
import org.erwinkok.quic.common.quiche.NativeHelper.C_INT
import org.erwinkok.quic.common.quiche.NativeHelper.C_POINTER
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment

private val logger = KotlinLogging.logger {}

@Suppress("ktlint:standard:function-naming")
object Quiche {
    private const val EXPECTED_QUICHE_VERSION = "0.24.4"

    init {
        val quicheVersion = quiche_version().getUtf8String(0L)
        require(quicheVersion == EXPECTED_QUICHE_VERSION) {
            "Unexpected Native Quiche version: $quicheVersion, expected $EXPECTED_QUICHE_VERSION"
        }
        if (logger.isDebugEnabled()) {
            logger.debug { "Loaded Native Quiche version: $quicheVersion" }

            val cb = NativeHelper.upcallMemorySegment<LoggingCallback>(
                "log",
                LoggingCallback.INSTANCE,
                FunctionDescriptor.ofVoid(C_POINTER, C_POINTER),
                LoggingCallback.SCOPE,
            )
            require(quiche_enable_debug_logging(cb, MemorySegment.NULL) == 0) {
                "Cannot enable quiche debug logging"
            }
        }
    }

    private class LoggingCallback {
        companion object Companion {
            val INSTANCE: LoggingCallback = LoggingCallback()
            val SCOPE: Arena = Arena.ofAuto()
        }

        fun log(msg: MemorySegment, argp: MemorySegment?) {
            logger.debug { "[Quiche] ${msg.getUtf8String(0L)}" }
        }
    }

    fun quiche_version(): MemorySegment {
        return DowncallHandles.quiche_version.invokeExact() as MemorySegment
    }

    fun quiche_enable_debug_logging(cb: MemorySegment, argp: MemorySegment): Int {
        return DowncallHandles.quiche_enable_debug_logging.invokeExact(cb, argp) as Int
    }

    private object DowncallHandles {
        val quiche_version = NativeHelper.downcallHandle(
            "quiche_version",
            FunctionDescriptor.of(C_POINTER),
        )
        val quiche_enable_debug_logging = NativeHelper.downcallHandle(
            "quiche_enable_debug_logging",
            FunctionDescriptor.of(
                C_INT,
                NativeHelper.C_POINTER,
                NativeHelper.C_POINTER,
            ),
        )
    }
}
