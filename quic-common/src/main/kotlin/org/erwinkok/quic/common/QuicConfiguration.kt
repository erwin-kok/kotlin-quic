package org.erwinkok.quic.common

import java.nio.file.Path

enum class CongestionControlAlgorithm(val value: Int) {
    QUICHE_CC_RENO(0),
    QUICHE_CC_CUBIC(1),
    QUICHE_CC_BBR(2),
    QUICHE_CC_BBR2(3),
}

class QuicConfiguration(
    val verifyPeer: Boolean? = null,
    val trustedCertificatesPemPath: Path? = null,
    val certificateChainPemPath: Path? = null,
    val privateKeyPemPath: Path? = null,
    val applicationProtocols: List<String>? = null,
    val congestionControlAlgorithm: CongestionControlAlgorithm? = null,
    val maxIdleTimeout: Long? = null,
    val initialMaxData: Long? = null,
    val initialMaxStreamDataBidiLocal: Long? = null,
    val initialMaxStreamDataBidiRemote: Long? = null,
    val initialMaxStreamDataUni: Long? = null,
    val initialMaxStreamsBidi: Long? = null,
    val initialMaxStreamsUni: Long? = null,
    val disableActiveMigration: Boolean? = null,
    val maxConnectionWindow: Long? = null,
    val maxStreamWindow: Long? = null,
    val activeConnectionIdLimit: Long? = null,
    val maxReceiveUdpPayloadSize: Long? = null,
    val maxSendUdpPayloadSize: Long? = null,
)
