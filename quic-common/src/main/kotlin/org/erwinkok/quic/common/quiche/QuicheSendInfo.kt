package org.erwinkok.quic.common.quiche

import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator

object QuicheSendInfo {
    fun allocate(scope: SegmentAllocator): MemorySegment {
        return scope.allocate(LAYOUT)
    }

    private val LAYOUT = MemoryLayout.structLayout(
        SocketAddressStorage.layout().withName("from"),
        NativeHelper.C_INT.withName("from_len"),
        MemoryLayout.paddingLayout(4),
        SocketAddressStorage.layout().withName("to"),
        NativeHelper.C_INT.withName("to_len"),
        MemoryLayout.paddingLayout(4),
        TimeSpec.layout().withName("at"),
    )
}
