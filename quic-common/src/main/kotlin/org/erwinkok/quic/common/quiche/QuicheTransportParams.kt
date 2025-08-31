package org.erwinkok.quic.common.quiche

import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator

object QuicheTransportParams {
    fun allocate(scope: SegmentAllocator): MemorySegment {
        return scope.allocate(LAYOUT)
    }

    fun getPeerInitialMaxStreamsBidi(quicheTransportParams: MemorySegment): Long {
        return peer_initial_max_streams_bidi.get(quicheTransportParams, 0L) as Long
    }

    private val LAYOUT = MemoryLayout.structLayout(
        NativeHelper.C_LONG.withName("peer_max_idle_timeout"),
        NativeHelper.C_LONG.withName("peer_max_udp_payload_size"),
        NativeHelper.C_LONG.withName("peer_initial_max_data"),
        NativeHelper.C_LONG.withName("peer_initial_max_stream_data_bidi_local"),
        NativeHelper.C_LONG.withName("peer_initial_max_stream_data_bidi_remote"),
        NativeHelper.C_LONG.withName("peer_initial_max_stream_data_uni"),
        NativeHelper.C_LONG.withName("peer_initial_max_streams_bidi"),
        NativeHelper.C_LONG.withName("peer_initial_max_streams_uni"),
        NativeHelper.C_LONG.withName("peer_ack_delay_exponent"),
        NativeHelper.C_LONG.withName("peer_max_ack_delay"),
        NativeHelper.C_BOOL.withName("peer_disable_active_migration"),
        MemoryLayout.paddingLayout(7),
        NativeHelper.C_LONG.withName("peer_active_conn_id_limit"),
        NativeHelper.C_LONG.withName("peer_max_datagram_frame_size"),
    )

    private val peer_initial_max_streams_bidi = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("peer_initial_max_streams_bidi"))
}
