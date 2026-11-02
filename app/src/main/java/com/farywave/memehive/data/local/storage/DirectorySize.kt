package com.farywave.memehive.data.local.storage

import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal suspend fun getDirectorySizeBytes(
    directory: File,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
): Long =
    withContext(ioDispatcher) {
        directory.walkBottomUp().sumOf {
            ensureActive()
            it.length()
        }
    }
