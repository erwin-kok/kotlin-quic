package org.erwinkok.quic.common

import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator
import java.lang.foreign.ValueLayout.JAVA_LONG

class BinaryDataHolder private constructor(
    val bytes: ByteArray,
    val segment: MemorySegment,
    val segmentLength: MemorySegment,
) {
    private val hashCode = bytes.contentHashCode()

    val size = bytes.size
    val isEmpty = bytes.isEmpty()

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is BinaryDataHolder) {
            return super.equals(other)
        }
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        return hashCode
    }

    override fun toString(): String {
        return "[${bytes.toHexString(hexFormat)}]"
    }

    companion object Companion {
        private val hexFormat = HexFormat {
            bytes {
                bytesPerGroup = 1
                groupSeparator = ":"
            }
        }
        val NULL = BinaryDataHolder(byteArrayOf(), MemorySegment.NULL, MemorySegment.NULL)

        fun of(bytes: ByteArray, arena: SegmentAllocator): BinaryDataHolder {
            val length = bytes.size.toLong()
            val segment = arena.allocate(length)
            segment.copyFrom(MemorySegment.ofArray(bytes))
            val lengthSegment = arena.allocate(JAVA_LONG.byteSize())
            lengthSegment.set(JAVA_LONG, 0L, length)
            return BinaryDataHolder(bytes, segment, lengthSegment)
        }
    }
}
