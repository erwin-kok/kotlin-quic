package org.erwinkok.quic.common

import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.MemorySegment.ofArray

fun ByteArray.toMemorySegment(arena: Arena): MemorySegment {
    val segment = arena.allocate(this.size.toLong())
    return segment.copyFrom(ofArray(this))
}

fun MemorySegment.toPacket(length: Int): Source {
    val bytes = ByteArray(length)
    this.asByteBuffer().get(bytes)
    return buildPacket {
        write(bytes)
    }
}

fun MemorySegment.toByteArray(length: Int): ByteArray {
    val bytes = ByteArray(length)
    this.asByteBuffer().get(bytes)
    return bytes
}
