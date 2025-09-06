package org.erwinkok.quic.server

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.toJavaAddress
import kotlinx.io.Source
import kotlinx.io.readByteArray
import org.erwinkok.quic.common.quiche.Quiche
import org.erwinkok.quic.common.quiche.QuicheRecvInfo
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

private val logger = KotlinLogging.logger {}

class QuicheServerConnection(
    private val arena: Arena,
    private val quicheConnection: MemorySegment,
) : AutoCloseable {
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
                logger.error { "failed to process packet: $received" }
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

    override fun close() {
        arena.close()
    }
}
