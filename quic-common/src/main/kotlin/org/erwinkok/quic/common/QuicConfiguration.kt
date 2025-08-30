package org.erwinkok.quic.common

import java.nio.file.Path

enum class CongestionControlAlgorithm {
    QUICHE_CC_RENO,
    QUICHE_CC_CUBIC,
    QUICHE_CC_BBR,
    QUICHE_CC_BBR2,
}

class QuicConfiguration(
    val privateKeyPemPath: Path? = null,
    val certificateChainPemPath: Path? = null,
    val trustedCertificatesPemPath: Path? = null,
    val applicationProtocols: List<String>? = null,
    val maxIdleTimeout: Int? = null,
    val maxReceiveUdpPayloadSize: Int? = null,
    val maxSendUdpPayloadSize: Int? = null,
    val initialMaxData: Int? = null,
    val initialMaxStreamDataBidiLocal: Int? = null,
    val initialMaxStreamDataBidiRemote: Int? = null,
    val initialMaxStreamsBidi: Int? = null,
    val congestionControlAlgorithm: CongestionControlAlgorithm? = null,
)
