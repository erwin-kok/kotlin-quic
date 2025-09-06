package org.erwinkok.quic.common

enum class QuicTransportError {
    PROTOCOL_VIOLATION,
}

class QuicException(message: String, val error: QuicTransportError) : Exception(message)
