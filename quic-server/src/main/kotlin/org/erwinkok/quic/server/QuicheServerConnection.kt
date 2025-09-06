package org.erwinkok.quic.server

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.toJavaAddress
import kotlinx.io.Source
import kotlinx.io.readByteArray
import org.erwinkok.quic.common.CloseInfo
import org.erwinkok.quic.common.QuicError
import org.erwinkok.quic.common.quiche.Quiche
import org.erwinkok.quic.common.quiche.QuicheError
import org.erwinkok.quic.common.quiche.QuicheRecvInfo
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.foreign.ValueLayout.JAVA_CHAR
import java.lang.foreign.ValueLayout.JAVA_LONG
import java.nio.charset.StandardCharsets
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = KotlinLogging.logger {}

class QuicheServerConnection(
    private val arena: Arena,
    private val quicheConnection: MemorySegment,
) : AutoCloseable {
    private val lock = ReentrantLock()
    private val recvInfo = QuicheRecvInfo.allocate(arena)

    fun feedCypherBytes(inputSource: Source, inetLocalAddress: InetSocketAddress, remoteAddress: InetSocketAddress) {
        Arena.ofConfined().use { tempArena ->
            val bytes = inputSource.readByteArray()
            val segment = tempArena.allocate(bytes.size.toLong())
            segment.copyFrom(MemorySegment.ofArray(bytes))
            val local = inetLocalAddress.toJavaAddress()
            val peer = remoteAddress.toJavaAddress()
            QuicheRecvInfo.setSocketAddress(recvInfo, local, peer, tempArena)
            val received = Quiche.quiche_conn_recv(
                quicheConnection,
                segment,
                segment.byteSize(),
                recvInfo,
            )
            if (received < 0) {
                val quicheError = QuicheError.errorString(received.toInt())
                val quicError = QuicError.errorString(localCloseInfo()!!.error)
                logger.error { "failed to process packet: $received ($quicheError/$quicError)" }
            } else if (Quiche.quiche_conn_is_established(quicheConnection)) {
//                            val quiche_stream_iter = Quiche.quiche_conn_readable(connection.quicheConnection)
//                            val streamIdSegment = arena.allocate(JAVA_LONG)
//                            while (Quiche.quiche_stream_iter_next(quiche_stream_iter, streamIdSegment)) {
//                                val streamId = streamIdSegment.get(JAVA_LONG, 0L)
//                                logger.info { "stream $streamId is readable" }
//
//                            }
            }
        }
    }

    fun localCloseInfo(): CloseInfo? {
        lock.withLock {
            Arena.ofConfined().use { scope ->
                val JAVA_POINTER = ValueLayout.ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(Long.Companion.MAX_VALUE, ValueLayout.JAVA_BYTE))
                val app = scope.allocate(JAVA_CHAR)
                val error = scope.allocate(JAVA_LONG)
                val reason = scope.allocate(JAVA_POINTER)
                val reasonLength = scope.allocate(JAVA_LONG)
                if (Quiche.quiche_conn_local_error(quicheConnection, app, error, reason, reasonLength)) {
                    val errorValue = error.get(JAVA_LONG, 0L)
                    val reasonLengthValue = reasonLength.get(JAVA_LONG, 0L)
                    val reasonValue: String?
                    if (reasonLengthValue == 0L) {
                        return CloseInfo(errorValue.toInt(), null)
                    } else {
                        val reasonBytes = ByteArray(reasonLengthValue.toInt())
                        // dereference reason pointer
                        reason.get(JAVA_POINTER, 0L).reinterpret(reasonLengthValue).asByteBuffer().get(reasonBytes)
                        reasonValue = String(reasonBytes, StandardCharsets.UTF_8)
                        return CloseInfo(errorValue.toInt(), reasonValue)
                    }
                }
                return null
            }
        }
    }

    override fun close() {
        arena.close()
    }
}
