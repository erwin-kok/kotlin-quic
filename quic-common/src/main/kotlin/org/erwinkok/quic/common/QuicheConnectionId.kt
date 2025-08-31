package org.erwinkok.quic.common

class QuicheConnectionId(val connectionId: ByteArray) {
    private val hashCode = connectionId.contentHashCode()

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is QuicheConnectionId) {
            return super.equals(other)
        }
        return connectionId.contentEquals(other.connectionId)
    }

    override fun hashCode(): Int {
        return hashCode
    }

    override fun toString(): String {
        val hexString = connectionId.toHexString(
            HexFormat {
                bytes {
                    bytesPerGroup = 1
                    groupSeparator = ":"
                }
            },
        )
        return "[$hexString]"
    }
}
