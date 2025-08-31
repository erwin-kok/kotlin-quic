package org.erwinkok.quic.common.quiche

import org.erwinkok.quic.common.quiche.NativeHelper.C_BYTE
import org.erwinkok.quic.common.quiche.NativeHelper.C_INT
import org.erwinkok.quic.common.quiche.NativeHelper.C_SHORT
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder.nativeOrder

fun SocketAddress.convert(allocator: SegmentAllocator): MemorySegment {
    return when (NativeHelper.Platform) {
        NativeHelper.PlatformType.LINUX -> SocketAddressLinux.convert(this, allocator)
        NativeHelper.PlatformType.MAC -> SocketAddressMacOS.convert(this, allocator)
        NativeHelper.PlatformType.WINDOWS -> SocketAddressWindows.convert(this, allocator)
    }
}

private object SocketAddressLinux {
    private const val AF_INET: Short = 2
    private const val AF_INET6: Short = 10

    private val LAYOUT_INET4 = MemoryLayout.structLayout(
        C_SHORT.withName("sin_family"),
        C_SHORT.withName("sin_port"),
        C_INT.withName("sin_addr"),
        MemoryLayout.sequenceLayout(8, C_BYTE).withName("sin_zero"),
    ).withName("sockaddr_in")

    private val sin_family = LAYOUT_INET4.varHandle(MemoryLayout.PathElement.groupElement("sin_family"))
    private val sin_port = LAYOUT_INET4.varHandle(MemoryLayout.PathElement.groupElement("sin_port"))
    private val sin_addr = LAYOUT_INET4.varHandle(MemoryLayout.PathElement.groupElement("sin_addr"))

    private val LAYOUT_INET6 = MemoryLayout.structLayout(
        C_SHORT.withName("sin6_family"),
        C_SHORT.withName("sin6_port"),
        C_INT.withName("sin6_flowinfo"),
        MemoryLayout.sequenceLayout(16, C_BYTE).withName("sin6_addr"),
        C_INT.withName("sin6_scope_id"),
    ).withName("sockaddr_in6")

    private val sin6_family = LAYOUT_INET6.varHandle(MemoryLayout.PathElement.groupElement("sin6_family"))
    private val sin6_port = LAYOUT_INET6.varHandle(MemoryLayout.PathElement.groupElement("sin6_port"))
    private val sin6_scope_id = LAYOUT_INET6.varHandle(MemoryLayout.PathElement.groupElement("sin6_scope_id"))
    private val sin6_flowinfo = LAYOUT_INET6.varHandle(MemoryLayout.PathElement.groupElement("sin6_flowinfo"))

    fun convert(socketAddress: SocketAddress, scope: SegmentAllocator): MemorySegment {
        require(socketAddress is InetSocketAddress) { "Only InetSocketAddress is supported" }
        return when (socketAddress.address) {
            is Inet4Address -> convertInet4(scope, socketAddress)
            is Inet6Address -> convertInet6(scope, socketAddress)
            else -> throw UnsupportedOperationException("Unsupported InetAddress: ${socketAddress.address}")
        }
    }

    private fun convertInet4(scope: SegmentAllocator, inetSocketAddress: InetSocketAddress): MemorySegment {
        val sin = scope.allocate(LAYOUT_INET4)
        sin_family.set(sin, AF_INET)
        sin_port.set(sin, inetSocketAddress.port.toShort())
        sin_addr.set(sin, ByteBuffer.wrap(inetSocketAddress.address.address).getInt())
        return sin
    }

    private fun convertInet6(scope: SegmentAllocator, inetSocketAddress: InetSocketAddress): MemorySegment {
        val sin6 = scope.allocate(LAYOUT_INET6)
        sin6_family.set(sin6, AF_INET6)
        sin6_port.set(sin6, inetSocketAddress.port.toShort())
        sin6.asSlice(8, 16).asByteBuffer().order(nativeOrder()).put(inetSocketAddress.address.address)
        sin6_scope_id.set(sin6, 0)
        sin6_flowinfo.set(sin6, 0)
        return sin6
    }
}

private object SocketAddressMacOS {
    private const val AF_INET: Byte = 2
    private const val AF_INET6: Byte = 30

    private val LAYOUT_INET4 = MemoryLayout.structLayout(
        C_BYTE.withName("sin_len"),
        C_BYTE.withName("sin_family"),
        C_SHORT.withName("sin_port"),
        C_INT.withName("sin_addr"),
        MemoryLayout.sequenceLayout(8, C_BYTE).withName("sin_zero"),
    ).withName("sockaddr_in")

    private val sin_len = LAYOUT_INET4.varHandle(MemoryLayout.PathElement.groupElement("sin_len"))
    private val sin_family = LAYOUT_INET4.varHandle(MemoryLayout.PathElement.groupElement("sin_family"))
    private val sin_port = LAYOUT_INET4.varHandle(MemoryLayout.PathElement.groupElement("sin_port"))
    private val sin_addr = LAYOUT_INET4.varHandle(MemoryLayout.PathElement.groupElement("sin_addr"))

    private val LAYOUT_INET6 = MemoryLayout.structLayout(
        C_BYTE.withName("sin6_len"),
        C_BYTE.withName("sin6_family"),
        C_SHORT.withName("sin6_port"),
        C_INT.withName("sin6_flowinfo"),
        MemoryLayout.sequenceLayout(16, C_BYTE).withName("sin6_addr"),
        C_INT.withName("sin6_scope_id"),
    ).withName("sockaddr_in6")

    private val sin6_len = LAYOUT_INET6.varHandle(MemoryLayout.PathElement.groupElement("sin6_len"))
    private val sin6_family = LAYOUT_INET6.varHandle(MemoryLayout.PathElement.groupElement("sin6_family"))
    private val sin6_port = LAYOUT_INET6.varHandle(MemoryLayout.PathElement.groupElement("sin6_port"))
    private val sin6_scope_id = LAYOUT_INET6.varHandle(MemoryLayout.PathElement.groupElement("sin6_scope_id"))
    private val sin6_flowinfo = LAYOUT_INET6.varHandle(MemoryLayout.PathElement.groupElement("sin6_flowinfo"))

    fun convert(socketAddress: SocketAddress, scope: SegmentAllocator): MemorySegment {
        require(socketAddress is InetSocketAddress) { "Only InetSocketAddress is supported" }
        return when (socketAddress.address) {
            is Inet4Address -> convertInet4(scope, socketAddress)
            is Inet6Address -> convertInet6(scope, socketAddress)
            else -> throw UnsupportedOperationException("Unsupported InetAddress: ${socketAddress.address}")
        }
    }

    private fun convertInet4(scope: SegmentAllocator, inetSocketAddress: InetSocketAddress): MemorySegment {
        val sin = scope.allocate(LAYOUT_INET4)
        sin_len.set(sin, sin.byteSize().toByte())
        sin_family.set(sin, AF_INET)
        sin_port.set(sin, inetSocketAddress.port.toShort())
        sin_addr.set(sin, ByteBuffer.wrap(inetSocketAddress.address.address).getInt())
        return sin
    }

    private fun convertInet6(scope: SegmentAllocator, inetSocketAddress: InetSocketAddress): MemorySegment {
        val sin6 = scope.allocate(LAYOUT_INET6.byteSize(), LAYOUT_INET6.byteAlignment())
        sin6_len.set(sin6, sin6.byteSize().toByte())
        sin6_family.set(sin6, AF_INET6)
        sin6_port.set(sin6, inetSocketAddress.port.toShort())
        sin6.asSlice(8, 16).asByteBuffer().order(nativeOrder()).put(inetSocketAddress.address.address)
        sin6_scope_id.set(sin6, 0)
        sin6_flowinfo.set(sin6, 0)
        return sin6
    }
}

private object SocketAddressWindows {
    private const val AF_INET: Short = 2
    private const val AF_INET6: Short = 23

    private val LAYOUT_INET4 = MemoryLayout.structLayout(
        C_SHORT.withName("sin_family"),
        C_SHORT.withName("sin_port"),
        C_INT.withName("sin_addr"),
        MemoryLayout.sequenceLayout(8, C_BYTE).withName("sin_zero"),
    ).withName("sockaddr_in")

    private val sin_family = LAYOUT_INET4.varHandle(MemoryLayout.PathElement.groupElement("sin_family"))
    private val sin_port = LAYOUT_INET4.varHandle(MemoryLayout.PathElement.groupElement("sin_port"))
    private val sin_addr = LAYOUT_INET4.varHandle(MemoryLayout.PathElement.groupElement("sin_addr"))

    private val LAYOUT_INET6 = MemoryLayout.structLayout(
        C_SHORT.withName("sin6_family"),
        C_SHORT.withName("sin6_port"),
        C_INT.withName("sin6_flowinfo"),
        MemoryLayout.sequenceLayout(16, C_BYTE).withName("sin6_addr"),
        C_INT.withName("sin6_scope_id"),
    ).withName("sockaddr_in6")

    private val sin6_family = LAYOUT_INET6.varHandle(MemoryLayout.PathElement.groupElement("sin6_family"))
    private val sin6_port = LAYOUT_INET6.varHandle(MemoryLayout.PathElement.groupElement("sin6_port"))
    private val sin6_scope_id = LAYOUT_INET6.varHandle(MemoryLayout.PathElement.groupElement("sin6_scope_id"))
    private val sin6_flowinfo = LAYOUT_INET6.varHandle(MemoryLayout.PathElement.groupElement("sin6_flowinfo"))

    fun convert(socketAddress: SocketAddress, scope: SegmentAllocator): MemorySegment {
        require(socketAddress is InetSocketAddress) { "Only InetSocketAddress is supported" }
        return when (socketAddress.address) {
            is Inet4Address -> convertInet4(scope, socketAddress)
            is Inet6Address -> convertInet6(scope, socketAddress)
            else -> throw UnsupportedOperationException("Unsupported InetAddress: ${socketAddress.address}")
        }
    }

    private fun convertInet4(scope: SegmentAllocator, inetSocketAddress: InetSocketAddress): MemorySegment {
        val sin = scope.allocate(LAYOUT_INET4)
        sin_family.set(sin, AF_INET)
        sin_port.set(sin, inetSocketAddress.port.toShort())
        sin_addr.set(sin, ByteBuffer.wrap(inetSocketAddress.address.address).getInt())
        return sin
    }

    private fun convertInet6(scope: SegmentAllocator, inetSocketAddress: InetSocketAddress): MemorySegment {
        val sin6 = scope.allocate(LAYOUT_INET6)
        sin6_family.set(sin6, AF_INET6)
        sin6_port.set(sin6, inetSocketAddress.port.toShort())
        sin6.asSlice(8, 16).asByteBuffer().order(nativeOrder()).put(inetSocketAddress.address.address)
        sin6_scope_id.set(sin6, 0)
        sin6_flowinfo.set(sin6, 0)
        return sin6
    }
}
