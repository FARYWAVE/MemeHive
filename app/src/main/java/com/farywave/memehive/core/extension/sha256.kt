package com.farywave.memehive.core.extension

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

suspend fun Uri.sha256(context: Context): String {
    return withContext(Dispatchers.IO) {

        val digest = MessageDigest.getInstance("SHA-256")

        context.contentResolver
            .openInputStream(this@sha256)
            ?.use { stream ->

                val buffer = ByteArray(8192)

                while (true) {
                    val read = stream.read(buffer)

                    if (read == -1) break

                    digest.update(buffer, 0, read)
                }
            }
            ?: throw IllegalStateException(
                "Failed to open URI"
            )

        digest.digest()
            .joinToString("") {
                "%02x".format(it)
            }
    }
}