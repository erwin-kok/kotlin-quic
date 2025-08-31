package org.erwinkok.quic.common.quiche

enum class QuicheError(val value: Int, val message: String) {
    // There is no more work to do.
    QUICHE_ERR_DONE(-1, "QUICHE_ERR_DONE"),

    // The provided buffer is too short.
    QUICHE_ERR_BUFFER_TOO_SHORT(-2, "QUICHE_ERR_BUFFER_TOO_SHORT"),

    // The provided packet cannot be parsed because its version is unknown.
    QUICHE_ERR_UNKNOWN_VERSION(-3, "QUICHE_ERR_UNKNOWN_VERSION"),

    // The provided packet cannot be parsed because it contains an invalid
    // frame.
    QUICHE_ERR_INVALID_FRAME(-4, "QUICHE_ERR_INVALID_FRAME"),

    // The provided packet cannot be parsed.
    QUICHE_ERR_INVALID_PACKET(-5, "QUICHE_ERR_INVALID_PACKET"),

    // The operation cannot be completed because the connection is in an
    // invalid state.
    QUICHE_ERR_INVALID_STATE(-6, "QUICHE_ERR_INVALID_STATE"),

    // The operation cannot be completed because the stream is in an
    // invalid state.
    QUICHE_ERR_INVALID_STREAM_STATE(-7, "QUICHE_ERR_INVALID_STREAM_STATE"),

    // The peer's transport params cannot be parsed.
    QUICHE_ERR_INVALID_TRANSPORT_PARAM(-8, "QUICHE_ERR_INVALID_TRANSPORT_PARAM"),

    // A cryptographic operation failed.
    QUICHE_ERR_CRYPTO_FAIL(-9, "QUICHE_ERR_CRYPTO_FAIL"),

    // The TLS handshake failed.
    QUICHE_ERR_TLS_FAIL(-10, "QUICHE_ERR_TLS_FAIL"),

    // The peer violated the local flow control limits.
    QUICHE_ERR_FLOW_CONTROL(-11, "QUICHE_ERR_FLOW_CONTROL"),

    // The peer violated the local stream limits.
    QUICHE_ERR_STREAM_LIMIT(-12, "QUICHE_ERR_STREAM_LIMIT"),

    // The received data exceeds the stream's final size.
    QUICHE_ERR_FINAL_SIZE(-13, "QUICHE_ERR_FINAL_SIZE"),

    // Error in congestion control.
    QUICHE_ERR_CONGESTION_CONTROL(-14, "QUICHE_ERR_CONGESTION_CONTROL"),

    // The specified stream was stopped by the peer.
    QUICHE_ERR_STREAM_STOPPED(-15, "QUICHE_ERR_STREAM_STOPPED"),

    // The specified stream was reset by the peer.
    QUICHE_ERR_STREAM_RESET(-16, "QUICHE_ERR_STREAM_RESET"),

    // Too many identifiers were provided.
    QUICHE_ERR_ID_LIMIT(-17, "QUICHE_ERR_ID_LIMIT"),

    // Not enough available identifiers.
    QUICHE_ERR_OUT_OF_IDENTIFIERS(-18, "QUICHE_ERR_OUT_OF_IDENTIFIERS"),

    // Error in key update.
    QUICHE_ERR_KEY_UPDATE(-19, "QUICHE_ERR_KEY_UPDATE"),

    // The peer sent more data in CRYPTO frames than we can buffer.
    QUICHE_ERR_CRYPTO_BUFFER_EXCEEDED(-20, "QUICHE_ERR_CRYPTO_BUFFER_EXCEEDED"),

    // The peer sent an ACK frame with an invalid range.
    QUICHE_ERR_INVALID_ACK_RANGE(-21, "QUICHE_ERR_INVALID_ACK_RANGE"),

    // The peer sends an ACK frame for a skipped packet used for Optimistic ACK
    // mitigation.
    QUICHE_ERR_OPTIMISTIC_ACK_DETECTED(-22, "QUICHE_ERR_OPTIMISTIC_ACK_DETECTED"),
    ;

    companion object {
        fun errorString(error: Int): String {
            return entries.firstOrNull { it.value == error }?.message ?: "Unknown error: $error"
        }
    }
}
