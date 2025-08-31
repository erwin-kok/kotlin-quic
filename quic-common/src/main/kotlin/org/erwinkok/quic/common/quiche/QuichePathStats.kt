package org.erwinkok.quic.common.quiche

import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator

object QuichePathStats {
    fun getCwnd(stats: MemorySegment): Long {
        return cwnd.get(stats, 0L) as Long
    }

    fun allocate(scope: SegmentAllocator): MemorySegment {
        return scope.allocate(LAYOUT)
    }

    private val LAYOUT = MemoryLayout.structLayout(
        SocketAddressStorage.layout().withName("local_addr"),
        NativeHelper.C_INT.withName("local_addr_len"),
        MemoryLayout.paddingLayout(4),
        SocketAddressStorage.layout().withName("peer_addr"),
        NativeHelper.C_INT.withName("peer_addr_len"),
        MemoryLayout.paddingLayout(4),
        NativeHelper.C_LONG.withName("validation_state"),
        NativeHelper.C_BOOL.withName("active"),
        MemoryLayout.paddingLayout(7),
        NativeHelper.C_LONG.withName("recv"),
        NativeHelper.C_LONG.withName("sent"),
        NativeHelper.C_LONG.withName("lost"),
        NativeHelper.C_LONG.withName("retrans"),
        NativeHelper.C_LONG.withName("rtt"),
        NativeHelper.C_LONG.withName("min_rtt"),
        NativeHelper.C_LONG.withName("rttvar"),
        NativeHelper.C_LONG.withName("cwnd"),
        NativeHelper.C_LONG.withName("sent_bytes"),
        NativeHelper.C_LONG.withName("recv_bytes"),
        NativeHelper.C_LONG.withName("lost_bytes"),
        NativeHelper.C_LONG.withName("stream_retrans_bytes"),
        NativeHelper.C_LONG.withName("pmtu"),
        NativeHelper.C_LONG.withName("delivery_rate"),
    )

    private val cwnd = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("cwnd"))
}
