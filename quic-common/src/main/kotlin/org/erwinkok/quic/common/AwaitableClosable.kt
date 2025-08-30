package org.erwinkok.quic.common

import io.ktor.utils.io.core.Closeable
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job

interface AwaitableClosable : Closeable {
    val jobContext: Job
    val isClosed: Boolean get() = jobContext.isCompleted

    suspend fun awaitClosed() {
        jobContext.join()

        @OptIn(InternalCoroutinesApi::class)
        if (jobContext.isCancelled) {
            throw jobContext.getCancellationException()
        }
    }
}
