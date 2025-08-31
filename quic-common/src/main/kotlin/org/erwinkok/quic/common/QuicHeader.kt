package org.erwinkok.quic.common

import io.github.oshai.kotlinlogging.KotlinLogging
import org.erwinkok.quic.common.Token.MAX_TOKEN_LENGTH
import org.erwinkok.quic.common.quiche.NativeHelper
import org.erwinkok.quic.common.quiche.Quiche
import org.erwinkok.quic.common.quiche.QuicheConstants.QUICHE_MAX_CONN_ID_LEN
import org.erwinkok.quic.common.quiche.QuicheError
import java.io.IOException
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.nio.ByteBuffer

private val logger = KotlinLogging.logger {}

data class QuicHeader(
    val version: Int,
    val type: QuicPacketType,
    val scId: QuicheConnectionId,
    val dcId: QuicheConnectionId,
    val tokenBytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is QuicHeader) {
            return super.equals(other)
        }
        if (version != other.version) return false
        if (type != other.type) return false
        if (scId != other.scId) return false
        if (dcId != other.dcId) return false
        if (!tokenBytes.contentEquals(other.tokenBytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + type.hashCode()
        result = 31 * result + scId.hashCode()
        result = 31 * result + dcId.hashCode()
        result = 31 * result + tokenBytes.contentHashCode()
        return result
    }

    companion object {
        fun parse(packetRead: ByteBuffer): QuicHeader {
            val scope = Arena.ofConfined()
            return try {
                val type = scope.allocate(NativeHelper.C_BYTE)
                val version = scope.allocate(NativeHelper.C_INT)

                // Source Connection ID
                val scid = scope.allocate(QUICHE_MAX_CONN_ID_LEN.toLong())
                val scidLength = scope.allocate(NativeHelper.C_LONG)
                scidLength.set(NativeHelper.C_LONG, 0L, scid.byteSize())

                // Destination Connection ID
                val dcid = scope.allocate(QUICHE_MAX_CONN_ID_LEN.toLong())
                val dcidLength = scope.allocate(NativeHelper.C_LONG)
                dcidLength.set(NativeHelper.C_LONG, 0L, dcid.byteSize())

                val token = scope.allocate(MAX_TOKEN_LENGTH)
                val tokenLength = scope.allocate(NativeHelper.C_LONG)
                tokenLength.set(NativeHelper.C_LONG, 0L, token.byteSize())

                val rc = if (packetRead.isDirect) {
                    // If the ByteBuffer is direct, it can be used without any copy.
                    val packetReadSegment = MemorySegment.ofBuffer(packetRead)
                    Quiche.quiche_header_info(
                        packetReadSegment,
                        packetRead.remaining().toLong(),
                        QUICHE_MAX_CONN_ID_LEN.toLong(),
                        version,
                        type,
                        scid,
                        scidLength,
                        dcid,
                        dcidLength,
                        token,
                        tokenLength,
                    )
                } else {
                    val packetReadSegment = scope.allocate(packetRead.remaining().toLong())
                    val prevPosition = packetRead.position()
                    packetReadSegment.asByteBuffer().put(packetRead)
                    packetRead.position(prevPosition)
                    Quiche.quiche_header_info(
                        packetReadSegment,
                        packetRead.remaining().toLong(),
                        QUICHE_MAX_CONN_ID_LEN.toLong(),
                        version,
                        type,
                        scid,
                        scidLength,
                        dcid,
                        dcidLength,
                        token,
                        tokenLength,
                    )
                }
                if (rc < 0) {
                    throw IOException("failed to parse header: ${QuicheError.errorString(rc)}")
                }

                if (logger.isDebugEnabled()) {
                    logger.debug { "version: ${version.get(NativeHelper.C_INT, 0L)}" }
                    logger.debug { "type: ${type.get(NativeHelper.C_BYTE, 0L)}" }
                    logger.debug { "scid len: ${scidLength.get(NativeHelper.C_LONG, 0L)}" }
                    logger.debug { "dcid len: ${dcidLength.get(NativeHelper.C_LONG, 0L)}" }
                    logger.debug { "token len: ${tokenLength.get(NativeHelper.C_LONG, 0L)}" }
                }

                val quicType = QuicPacketType.of(type.get(NativeHelper.C_BYTE, 0L).toInt())

                val scIdBytes = ByteArray(scidLength.get(NativeHelper.C_LONG, 0L).toInt())
                scid.asByteBuffer().get(scIdBytes)

                val dcIdBytes = ByteArray(dcidLength.get(NativeHelper.C_LONG, 0L).toInt())
                dcid.asByteBuffer().get(dcIdBytes)

                val tokenBytes = ByteArray(tokenLength.get(NativeHelper.C_LONG, 0L).toInt())
                token.asByteBuffer().get(tokenBytes)

                QuicHeader(
                    version.get(NativeHelper.C_INT, 0L),
                    quicType,
                    QuicheConnectionId(scIdBytes),
                    QuicheConnectionId(dcIdBytes),
                    tokenBytes,
                )
            } finally {
                scope.close()
            }
        }
    }
}
