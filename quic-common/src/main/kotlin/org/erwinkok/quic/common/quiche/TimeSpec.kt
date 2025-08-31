package org.erwinkok.quic.common.quiche

import java.lang.foreign.MemoryLayout

object TimeSpec {
    private val layout = MemoryLayout.structLayout(
        NativeHelper.C_LONG.withName("tv_sec"),
        NativeHelper.C_LONG.withName("tv_nsec"),
    )

    fun layout(): MemoryLayout {
        return layout
    }
}
