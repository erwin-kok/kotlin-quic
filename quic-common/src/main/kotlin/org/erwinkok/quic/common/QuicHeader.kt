package org.erwinkok.quic.common

import org.erwinkok.quic.common.QuicheConstants.MAX_TOKEN_LENGTH
import org.erwinkok.quic.common.QuicheConstants.QUICHE_MAX_CONN_ID_LEN
import org.erwinkok.quic.common.quiche.NativeHelper
import org.erwinkok.quic.common.quiche.Quiche
import org.erwinkok.quic.common.quiche.QuicheError
import java.io.IOException
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout.PathElement.groupElement
import java.lang.foreign.MemoryLayout.paddingLayout
import java.lang.foreign.MemoryLayout.sequenceLayout
import java.lang.foreign.MemoryLayout.structLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG

class QuicHeader private constructor(
    private val arena: Arena,
    private val memorySegment: MemorySegment,
) : AutoCloseable {
    val version by lazy {
        memorySegment.get(JAVA_INT, versionOffset)
    }
    val type by lazy {
        val type = memorySegment.get(JAVA_BYTE, typeOffset).toInt()
        QuicPacketType.of(type)
    }
    val scid by lazy {
        val scidBytes = ByteArray(scidLength)
        memorySegment.asSlice(scidOffset, scidLength.toLong()).asByteBuffer().get(scidBytes)
        QuicheConnectionId(scidBytes)
    }
    val scidLength by lazy {
        memorySegment.get(JAVA_LONG, scidLengthOffset).toInt()
    }
    val dcid by lazy {
        val dcidBytes = ByteArray(dcidLength)
        memorySegment.asSlice(dcidOffset, dcidLength.toLong()).asByteBuffer().get(dcidBytes)
        QuicheConnectionId(dcidBytes)
    }
    val dcidLength by lazy {
        memorySegment.get(JAVA_LONG, dcidLengthOffset).toInt()
    }
    val token by lazy {
        val tokenBytes = ByteArray(tokenLength)
        memorySegment.asSlice(tokenOffset, tokenLength.toLong()).asByteBuffer().get(tokenBytes)
        tokenBytes
    }
    val tokenLength by lazy {
        memorySegment.get(JAVA_LONG, tokenLengthOffset).toInt()
    }

    val versionSegment by lazy {
        memorySegment.asSlice(versionOffset, JAVA_INT.byteSize())
    }
    val typeSegment by lazy {
        memorySegment.asSlice(typeOffset, JAVA_BYTE.byteSize())
    }
    val scidSegment by lazy {
        memorySegment.asSlice(scidOffset, scidLength.toLong())
    }
    val dcidSegment by lazy {
        memorySegment.asSlice(dcidOffset, dcidLength.toLong())
    }
    val tokenSegment by lazy {
        memorySegment.asSlice(tokenOffset, tokenLength.toLong())
    }
    val size: Long by lazy {
        JAVA_INT.byteSize() + JAVA_BYTE.byteSize() + scidLength + dcidLength + tokenLength
    }

    override fun toString(): String {
        return "version=$version, type=$type, scid=$scid, dcid=$dcid, token=$token (size = $size)"
    }

    override fun close() {
        arena.close()
    }

    companion object {
        private val OUTPUT_LAYOUT = structLayout(
            JAVA_INT.withName("version"),
            JAVA_BYTE.withName("type"),
            paddingLayout(3),
            sequenceLayout(QUICHE_MAX_CONN_ID_LEN, JAVA_BYTE).withName("scid"),
            paddingLayout(4),
            JAVA_LONG.withName("scid_len"),
            sequenceLayout(QUICHE_MAX_CONN_ID_LEN, JAVA_BYTE).withName("dcid"),
            paddingLayout(4),
            JAVA_LONG.withName("dcid_len"),
            sequenceLayout(MAX_TOKEN_LENGTH, JAVA_BYTE).withName("token"),
            JAVA_LONG.withName("token_len"),
        )

        private val versionOffset = OUTPUT_LAYOUT.byteOffset(groupElement("version"))
        private val typeOffset = OUTPUT_LAYOUT.byteOffset(groupElement("type"))
        private val scidOffset = OUTPUT_LAYOUT.byteOffset(groupElement("scid"))
        private val scidLengthOffset = OUTPUT_LAYOUT.byteOffset(groupElement("scid_len"))
        private val dcidOffset = OUTPUT_LAYOUT.byteOffset(groupElement("dcid"))
        private val dcidLengthOffset = OUTPUT_LAYOUT.byteOffset(groupElement("dcid_len"))
        private val tokenOffset = OUTPUT_LAYOUT.byteOffset(groupElement("token"))
        private val tokenLengthOffset = OUTPUT_LAYOUT.byteOffset(groupElement("token_len"))

        fun parse(inputSegment: MemorySegment): QuicHeader {
            val arena = Arena.ofConfined()
            try {
                val outputSegment = arena.allocate(OUTPUT_LAYOUT)
                val scidLength = outputSegment.asSlice(scidLengthOffset)
                scidLength.set(NativeHelper.C_LONG, 0L, QUICHE_MAX_CONN_ID_LEN)
                val dcidLength = outputSegment.asSlice(dcidLengthOffset)
                dcidLength.set(NativeHelper.C_LONG, 0L, QUICHE_MAX_CONN_ID_LEN)
                val tokenLength = outputSegment.asSlice(tokenLengthOffset)
                tokenLength.set(NativeHelper.C_LONG, 0L, MAX_TOKEN_LENGTH)
                val rc = Quiche.quiche_header_info(
                    inputSegment,
                    inputSegment.byteSize(),
                    QUICHE_MAX_CONN_ID_LEN,
                    outputSegment.asSlice(versionOffset, JAVA_INT.byteSize()),
                    outputSegment.asSlice(typeOffset, JAVA_BYTE.byteSize()),
                    outputSegment.asSlice(scidOffset),
                    outputSegment.asSlice(scidLengthOffset, JAVA_LONG.byteSize()),
                    outputSegment.asSlice(dcidOffset),
                    outputSegment.asSlice(dcidLengthOffset, JAVA_LONG.byteSize()),
                    outputSegment.asSlice(tokenOffset),
                    outputSegment.asSlice(tokenLengthOffset, JAVA_LONG.byteSize()),
                )
                if (rc < 0) {
                    throw IOException("failed to parse header: ${QuicheError.errorString(rc)}")
                }
                return QuicHeader(arena, outputSegment)
            } catch (e: Exception) {
                arena.close()
                throw e
            }
        }
    }
}
