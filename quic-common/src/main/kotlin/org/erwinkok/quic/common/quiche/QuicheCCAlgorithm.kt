package org.erwinkok.quic.common.quiche

enum class QuicheCCAlgorithm(val value: Int) {
    QUICHE_CC_RENO(0),
    QUICHE_CC_CUBIC(1),
    QUICHE_CC_BBR(2),
}
