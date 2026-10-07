package com.farywave.memehive.data.local.storage

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class ImageChanges private constructor() {
    private val createdFiles = mutableSetOf<File>()

    @Synchronized
    internal fun recordCreated(file: File) {
        createdFiles.add(file)
    }

    @Synchronized
    private fun filesForCleanup(): List<File> = createdFiles.toList()

    internal companion object {
        suspend fun <T> track(
            ioDispatcher: CoroutineDispatcher,
            isStored: suspend (path: String) -> Boolean,
            onCleanupFailure: (File, Exception) -> Unit,
            block: suspend (ImageChanges) -> T,
        ): T {
            val changes = ImageChanges()
            try {
                return block(changes)
            } finally {
                withContext(NonCancellable) {
                    withContext(ioDispatcher) {
                        for (file in changes.filesForCleanup()) {
                            try {
                                if (!isStored(file.path) && !file.delete() && file.exists()) {
                                    throw IOException("Could not delete abandoned image: $file")
                                }
                            } catch (error: Exception) {
                                onCleanupFailure(file, error)
                            }
                        }
                    }
                }
            }
        }
    }
}
