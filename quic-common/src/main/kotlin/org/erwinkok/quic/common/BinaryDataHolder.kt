package org.erwinkok.quic.common

import java.lang.foreign.MemorySegment

class BinaryDataHolder(
    val bytes: ByteArray?,
    val segment: MemorySegment,
    val lengthSegment: MemorySegment,
) {
    private val hashCode = bytes.contentHashCode()

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is QuicheConnectionId) {
            return super.equals(other)
        }
        return bytes.contentEquals(other.connectionId)
    }

    override fun hashCode(): Int {
        return hashCode
    }

    override fun toString(): String {
        if (bytes == null) {
            return "[]"
        } else {
            val hexString = bytes.toHexString(hexFormat)
            return "[$hexString]"
        }
    }

    companion object Companion {
        private val hexFormat = HexFormat {
            bytes {
                bytesPerGroup = 1
                groupSeparator = ":"
            }
        }

        val NULL = BinaryDataHolder(null, MemorySegment.NULL, MemorySegment.NULL)
    }
}
