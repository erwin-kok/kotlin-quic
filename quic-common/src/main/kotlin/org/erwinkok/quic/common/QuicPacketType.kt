package org.erwinkok.quic.common

enum class QuicPacketType {
    INITIAL,
    RETRY,
    HANDSHAKE,
    ZERO_RTT,
    SHORT,
    VERSION_NEGOTIATION,
    ;

    companion object {
        fun of(value: Int) = when (value) {
            1 -> INITIAL
            2 -> RETRY
            3 -> HANDSHAKE
            4 -> ZERO_RTT
            5 -> SHORT
            6 -> VERSION_NEGOTIATION
            else -> throw IllegalArgumentException("Unknown QUIC packet type: $value")
        }
    }
}
