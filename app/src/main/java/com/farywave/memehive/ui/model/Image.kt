package com.farywave.memehive.ui.model

import android.content.Context
import android.net.Uri
import com.farywave.memehive.data.local.db.entity.ImageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

data class Image(
    val id: Long = 0,
    val uri: Uri
) {
    suspend fun toEntity(context: Context) = ImageEntity(id, uri.toString(), sha256(context))

    suspend fun sha256(
        context: Context
    ): String = withContext(Dispatchers.IO) {

        val digest = MessageDigest.getInstance("SHA-256")

        context.contentResolver
            .openInputStream(uri)
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