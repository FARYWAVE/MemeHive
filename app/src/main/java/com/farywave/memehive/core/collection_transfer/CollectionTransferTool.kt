package com.farywave.memehive.core.collection_transfer

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.farywave.memehive.core.collection_transfer.entity.TransferCollection
import com.farywave.memehive.core.collection_transfer.entity.TransferManifest
import com.farywave.memehive.core.collection_transfer.entity.TransferMediaItem
import com.farywave.memehive.data.local.db.relation.MediaItemDetails
import com.farywave.memehive.domain.model.Collection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CollectionTransferTool(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val json = Json {
        prettyPrint = true
    }

    suspend fun exportCollection(
        collection: Collection,
        mediaItems: List<MediaItemDetails>,
        outputUri: Uri
    ): Unit = withContext(ioDispatcher) {
        val tempDir = createTempExportDir()
        val collectionDir = File(tempDir, "collection")
        val mediaDir = File(tempDir, "media")

        createDirectory(collectionDir)
        createDirectory(mediaDir)

        //Collection Preparation
        val exportedCoverPath = "collection/cover.jpg"

        collection.cover?.let { cover ->

            val coverUri = cover.uri

            copyUriToFile(
                coverUri,
                File(tempDir, exportedCoverPath)
            )
        }

        //Media Preparation
        val transferMediaItems = mediaItems.filter { it.image != null }.mapIndexed { index, item ->
            ensureActive()
            val image = File(item.image!!.path)
            val extension = image.extension

            val exportName = "%03d.%s".format(index, extension)

            val relativePath = "media/$exportName"
            copyUriToFile(
                image.toUri(),
                File(tempDir, relativePath)
            )

            TransferMediaItem(
                path = relativePath,
                caption = item.mediaItem.caption,
                description = item.mediaItem.description,
                tags = item.tags.map { it.name }
            )
        }

        //Manifest Preparation
        val manifest = TransferManifest(
            appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionCode,
            exportedAt = System.currentTimeMillis(),
            collection = TransferCollection(
                name = collection.name,
                coverPath = collection.cover?.let {
                    "collection/cover.jpg"
                }
            ),
            mediaItems = transferMediaItems
        )

        //Manifest Serialization
        val manifestFile = File(tempDir, "manifest.json")

        manifestFile.writeText(
            json.encodeToString(manifest)
        )

        // Zip the directory
        val output = context.contentResolver.openOutputStream(outputUri)
            ?: throw IOException("Could not open the export destination.")
        output.use { zipDirectory(tempDir, it) }
    }

    suspend fun unzipCollection(uri: Uri): Pair<TransferManifest, File> = withContext(ioDispatcher) {
        val tempDir = File(context.cacheDir, "import_temp")

        if (tempDir.exists() && !tempDir.deleteRecursively()) {
            throw IOException("Could not clear the temporary import directory.")
        }

        createDirectory(tempDir)

        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open the collection archive.")
        val manifest = CollectionArchiveReader.extract(input, tempDir)
        manifest to tempDir
    }

    private fun createTempExportDir(): File {
        val dir = File(context.cacheDir, "export_temp")

        if (dir.exists() && !dir.deleteRecursively()) {
            throw IOException("Could not clear the temporary export directory.")
        }

        createDirectory(dir)

        return dir
    }

    private fun copyUriToFile(
        uri: Uri,
        destination: File
    ) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open an image for export.")
        input.use {
            destination.outputStream().use { output ->
                it.copyTo(output)
            }
        }
    }

    private fun createDirectory(directory: File) {
        if (!directory.mkdirs() && !directory.isDirectory) {
            throw IOException("Could not create directory: $directory")
        }
    }

    private suspend fun zipDirectory(
        sourceDir: File,
        outputStream: OutputStream
    ) {

        ZipOutputStream(
            BufferedOutputStream(outputStream)
        ).use { zipOut ->

            sourceDir.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    currentCoroutineContext().ensureActive()

                    val entryName = file
                        .relativeTo(sourceDir)
                        .path
                        .replace("\\", "/")

                    val entry = ZipEntry(entryName)

                    zipOut.putNextEntry(entry)

                    file.inputStream().use { input ->
                        input.copyTo(zipOut)
                    }

                    zipOut.closeEntry()
                }
        }
    }
}
