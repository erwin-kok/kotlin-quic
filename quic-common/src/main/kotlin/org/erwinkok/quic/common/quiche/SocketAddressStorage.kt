package org.erwinkok.quic.common.quiche

import java.lang.foreign.MemoryLayout

object SocketAddressStorage {
    fun layout(): MemoryLayout {
        return LAYOUT
    }

    private val LAYOUT = MemoryLayout.structLayout(
        NativeHelper.C_SHORT.withName("ss_family"),
        MemoryLayout.sequenceLayout(118, NativeHelper.C_BYTE).withName("__ss_padding"),
        NativeHelper.C_LONG.withName("__ss_align"),
    )
}
