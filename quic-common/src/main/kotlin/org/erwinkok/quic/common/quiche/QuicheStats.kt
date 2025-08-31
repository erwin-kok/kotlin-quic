package org.erwinkok.quic.common.quiche

import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator

object QuicheStats {
    fun allocate(scope: SegmentAllocator): MemorySegment {
        return scope.allocate(LAYOUT)
    }

    private val LAYOUT = MemoryLayout.structLayout(
        NativeHelper.C_LONG.withName("recv"),
        NativeHelper.C_LONG.withName("sent"),
        NativeHelper.C_LONG.withName("lost"),
        NativeHelper.C_LONG.withName("retrans"),
        NativeHelper.C_LONG.withName("sent_bytes"),
        NativeHelper.C_LONG.withName("recv_bytes"),
        NativeHelper.C_LONG.withName("acked_bytes"),
        NativeHelper.C_LONG.withName("lost_bytes"),
        NativeHelper.C_LONG.withName("stream_retrans_bytes"),
        NativeHelper.C_LONG.withName("paths_count"),
        NativeHelper.C_LONG.withName("reset_stream_count_local"),
        NativeHelper.C_LONG.withName("stopped_stream_count_local"),
        NativeHelper.C_LONG.withName("reset_stream_count_remote"),
        NativeHelper.C_LONG.withName("stopped_stream_count_remote"),
    )
}
