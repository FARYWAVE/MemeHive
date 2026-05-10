package com.farywave.memehive.core.collection_transfer

import android.content.Context
import android.net.Uri
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.MediaItem
import java.io.File

object CollectionTransferTool {

    suspend fun exportCollection(
        context: Context,
        collection: Collection,
        mediaItems: List<MediaItem>,
        outputUri: Uri
    ) {
        val tempDir = createTempExportDir(context)
        val collectionDir = File(tempDir, "collection")
        val mediaDir = File(tempDir, "media")

        collectionDir.mkdirs()
        mediaDir.mkdirs()

    }

    private fun createTempExportDir(context: Context): File {
        val dir = File(context.cacheDir, "export_temp")

        if (dir.exists()) {
            dir.deleteRecursively()
        }

        dir.mkdirs()

        return dir
    }

    private fun copyUriToFile(
        context: Context,
        uri: Uri,
        destination: File
    ) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}