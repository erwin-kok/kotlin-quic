package org.erwinkok.quic.common

object QuicheConstants {
    // The current QUIC wire version.
    const val QUICHE_PROTOCOL_VERSION = 0x00000001

    // The maximum length of a connection ID.
    const val QUICHE_MAX_CONN_ID_LEN = 20L

    // The minimum length of Initial packets sent by a client.
    const val QUICHE_MIN_CLIENT_INITIAL_LEN = 1200

    const val MAX_TOKEN_LENGTH = 48L
}
