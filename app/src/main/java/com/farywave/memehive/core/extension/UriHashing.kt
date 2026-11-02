package com.farywave.memehive.core.extension

import android.content.Context
import android.net.Uri
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

suspend fun Uri.sha256(context: Context, ioDispatcher: CoroutineDispatcher): String {
    return withContext(ioDispatcher) {
        val digest = MessageDigest.getInstance("SHA-256")

        context.contentResolver.openInputStream(this@sha256)?.use { stream ->
            val buffer = ByteArray(8192)

            while (true) {
                ensureActive()
                val read = stream.read(buffer)

                if (read == -1) break

                digest.update(buffer, 0, read)
            }
        } ?: throw IOException("Failed to open URI")

        digest.digest().joinToString("") {
            "%02x".format(it)
        }
    }
}
