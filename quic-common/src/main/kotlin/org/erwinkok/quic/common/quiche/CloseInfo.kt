package org.erwinkok.quic.common.quiche

data class CloseInfo(
    val error: Long,
    val reason: String?,
)
