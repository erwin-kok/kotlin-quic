package org.erwinkok.quic.server

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

class QuicheServerConnection(
    private val arena: Arena,
    val scidSegment: MemorySegment,
    val quicheConnection: MemorySegment,
) : AutoCloseable {

    override fun close() {
        arena.close()
    }
}
