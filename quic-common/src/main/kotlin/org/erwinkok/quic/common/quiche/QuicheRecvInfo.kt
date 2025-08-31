package org.erwinkok.quic.common.quiche

import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator
import java.net.SocketAddress

object QuicheRecvInfo {
    fun allocate(scope: SegmentAllocator): MemorySegment {
        return scope.allocate(LAYOUT)
    }

    fun setSocketAddress(recvInfo: MemorySegment, local: SocketAddress, peer: SocketAddress, scope: SegmentAllocator) {
        val peerSockAddrSegment = peer.convert(scope)
        from.set(recvInfo, peerSockAddrSegment)
        fromLength.set(recvInfo, peerSockAddrSegment.byteSize().toInt())
        val localSockAddrSegment = local.convert(scope)
        to.set(recvInfo, localSockAddrSegment)
        toLength.set(recvInfo, localSockAddrSegment.byteSize().toInt())
    }

    private val LAYOUT = MemoryLayout.structLayout(
        NativeHelper.C_POINTER.withName("from"),
        NativeHelper.C_INT.withName("from_len"),
        MemoryLayout.paddingLayout(4),
        NativeHelper.C_POINTER.withName("to"),
        NativeHelper.C_INT.withName("to_len"),
        MemoryLayout.paddingLayout(4),
    )

    private val from = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("from"))
    private val fromLength = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("from_len"))
    private val to = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("to"))
    private val toLength = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("to_len"))
}
