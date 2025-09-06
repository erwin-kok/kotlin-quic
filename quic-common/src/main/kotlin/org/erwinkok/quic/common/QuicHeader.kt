package org.erwinkok.quic.common

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.utils.io.core.remaining
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.io.readUInt
import org.erwinkok.quic.common.QuicheConstants.QUICHE_MAX_CONN_ID_LEN
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator
import java.lang.foreign.ValueLayout.JAVA_LONG

private val logger = KotlinLogging.logger {}

class QuicHeader(
    private val arena: Arena,
    val version: Long,
    val type: QuicPacketType,
    val scid: BinaryDataHolder,
    val dcid: BinaryDataHolder,
    val token: BinaryDataHolder,
) : AutoCloseable {
    override fun toString(): String {
        return "version=$version, type=$type, scid=$scid, dcid=$dcid, token=$token"
    }

    override fun close() {
        arena.close()
    }

    companion object Companion {
        private const val AES_128_GCM_TAG_LENGTH = 16

        fun parse(inputSource: Source): QuicHeader {
            val arena = Arena.ofConfined()
            try {
                checkReadable(inputSource, Byte.SIZE_BYTES)
                val first = inputSource.readByte()
                return if (hasShortHeader(first)) {
                    // See https://www.rfc-editor.org/rfc/rfc9000.html#section-17.3
                    // 1-RTT Packet {
                    //  Header Form (1) = 0,
                    //  Fixed Bit (1) = 1,
                    //  Spin Bit (1),
                    //  Reserved Bits (2),
                    //  Key Phase (1),
                    //  Packet Number Length (2),
                    //  Destination Connection ID (0..160),
                    //  Packet Number (8..32),
                    //  Packet Payload (8..),
                    //}
                    val dcid = sliceBinaryArray(inputSource, arena, QUICHE_MAX_CONN_ID_LEN.toInt())
                    QuicHeader(arena, 0, QuicPacketType.SHORT, BinaryDataHolder.NULL, dcid, BinaryDataHolder.NULL)
                } else {
                    // See https://www.rfc-editor.org/rfc/rfc9000.html#section-17.2
                    // Long Header Packet {
                    //  Header Form (1) = 1,
                    //  Fixed Bit (1) = 1,
                    //  Long Packet Type (2),
                    //  Type-Specific Bits (4),
                    //  Version (32),
                    //  Destination Connection ID Length (8),
                    //  Destination Connection ID (0..160),
                    //  Source Connection ID Length (8),
                    //  Source Connection ID (0..160),
                    //  Type-Specific Payload (..),
                    //}
                    checkReadable(inputSource, Int.SIZE_BYTES + Byte.SIZE_BYTES)
                    val version = inputSource.readUInt().toLong()

                    val type = typeOfLongHeader(first, version)

                    val dcidLen = inputSource.readByte().toInt()
                    checkCidLength(dcidLen)
                    val dcid = sliceBinaryArray(inputSource, arena, dcidLen)

                    val scidLen = inputSource.readByte().toInt()
                    checkCidLength(scidLen)
                    val scid = sliceBinaryArray(inputSource, arena, scidLen)

                    val token = sliceToken(type, inputSource, arena)

                    QuicHeader(arena, version, type, scid, dcid, token)
                }
            } catch (e: Exception) {
                arena.close()
                throw e
            }
        }

        private fun typeOfLongHeader(first: Byte, version: Long): QuicPacketType {
            if (version == 0L) {
                // If we parsed a version of 0 we are sure it's a version negotiation packet:
                // https://www.rfc-editor.org/rfc/rfc9000.html#section-17.2.1
                //
                // This also means we should ignore everything that is left in 'first'.
                return QuicPacketType.VERSION_NEGOTIATION
            }
            val packetType = (first.toInt() and 0x30) shr 4
            return when (packetType) {
                0x00 -> QuicPacketType.INITIAL
                0x01 -> QuicPacketType.ZERO_RTT
                0x02 -> QuicPacketType.HANDSHAKE
                0x03 -> QuicPacketType.RETRY
                else -> throw QuicException("Unknown packet type: $packetType", QuicTransportError.PROTOCOL_VIOLATION)
            }
        }

        private fun sliceBinaryArray(inputSource: Source, arena: SegmentAllocator, length: Int): BinaryDataHolder {
            checkReadable(inputSource, length)
            val bytes = inputSource.readByteArray(length)
            val segment = arena.allocate(length.toLong())
            segment.copyFrom(MemorySegment.ofArray(bytes))
            val lengthSegment = arena.allocate(JAVA_LONG.byteSize())
            lengthSegment.set(JAVA_LONG, 0L, length.toLong())
            return BinaryDataHolder(bytes, segment, lengthSegment)
        }

        private fun sliceToken(type: QuicPacketType, inputSource: Source, arena: SegmentAllocator): BinaryDataHolder {
            when (type) {
                QuicPacketType.INITIAL -> {
                    checkReadable(inputSource, Byte.SIZE_BYTES)
                    val tokenLen = variableLengthInteger(inputSource).toInt()
                    return sliceBinaryArray(inputSource, arena, tokenLen)
                }

                QuicPacketType.RETRY -> {
                    // Exclude the integrity tag from the token.
                    // See https://www.rfc-editor.org/rfc/rfc9000.html#section-17.2.5
                    checkReadable(inputSource, AES_128_GCM_TAG_LENGTH)
                    val tokenLen = (inputSource.remaining - AES_128_GCM_TAG_LENGTH).toInt()
                    return sliceBinaryArray(inputSource, arena, tokenLen)
                }

                // No token included.
                else -> return BinaryDataHolder.NULL
            }
        }

        private fun variableLengthInteger(inputSource: Source): Long {
            checkReadable(inputSource, 1)
            // Peek the first byte, do not consume
            val first = inputSource.peek().readByte().toInt()
            val value = (first and 0xc0) shr 6
            return when (value) {
                0 -> {
                    inputSource.readByte().toLong()
                }

                1 -> {
                    checkReadable(inputSource, 2)
                    inputSource.readShort().toLong() and 0x3fffL
                }

                2 -> {
                    checkReadable(inputSource, 4)
                    inputSource.readInt().toLong() and 0x3fffffffL
                }

                3 -> {
                    checkReadable(inputSource, 8)
                    inputSource.readLong() and 0x3fffffffffffffffL
                }

                else -> {
                    throw QuicException("Unsupported length: $value", QuicTransportError.PROTOCOL_VIOLATION)
                }
            }
        }

        // Check if the connection id is not longer than 20. This is what is the maximum for QUIC version 1.
        // See https://www.rfc-editor.org/rfc/rfc9000.html#section-17.2
        private fun checkCidLength(length: Int) {
            if (length > QUICHE_MAX_CONN_ID_LEN) {
                throw QuicException("connection id to large: $length > $QUICHE_MAX_CONN_ID_LEN", QuicTransportError.PROTOCOL_VIOLATION)
            }
        }

        private fun checkReadable(inputSource: Source, needed: Int) {
            if (inputSource.remaining < needed) {
                throw QuicException("Not enough bytes to read, ${inputSource.remaining} < $needed", QuicTransportError.PROTOCOL_VIOLATION)
            }
        }

        private fun hasShortHeader(b: Byte): Boolean {
            return (b.toInt() and 0x80) == 0
        }
    }
}
