package com.farywave.memehive.core.collection_transfer

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.farywave.memehive.core.collection_transfer.entity.TransferCollection
import com.farywave.memehive.core.collection_transfer.entity.TransferManifest
import com.farywave.memehive.core.collection_transfer.entity.TransferMediaItem
import com.farywave.memehive.data.local.db.relation.MediaItemDetails
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.MediaItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class CollectionTransferTool(val context: Context) {
    private val json = Json {
        prettyPrint = true
    }

    fun exportCollection(
        collection: Collection,
        mediaItems: List<MediaItemDetails>,
        outputUri: Uri
    ) {
        val tempDir = createTempExportDir()
        val collectionDir = File(tempDir, "collection")
        val mediaDir = File(tempDir, "media")

        collectionDir.mkdirs()
        mediaDir.mkdirs()

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
        context.contentResolver
            .openOutputStream(outputUri)
            ?.use { output ->

                zipDirectory(tempDir, output)
            }
    }

    fun unzipCollection(uri: Uri): Pair<TransferManifest, File> {
        val tempDir = File(context.cacheDir, "import_temp")

        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }

        tempDir.mkdirs()

        //Import into Temp Directory
        ZipInputStream(
            BufferedInputStream(
                context.contentResolver.openInputStream(uri)
            )
        ).use { zipIn ->

            var entry = zipIn.nextEntry

            while (entry != null) {

                val outFile = File(tempDir, entry.name)

                val normalizedPath = outFile.canonicalPath
                val targetPath = tempDir.canonicalPath

                if (!normalizedPath.startsWith(targetPath)) {
                    throw SecurityException("Bad zip entry")
                }

                if (entry.isDirectory) {

                    outFile.mkdirs()

                } else {

                    outFile.parentFile?.mkdirs()

                    outFile.outputStream().use { output ->
                        zipIn.copyTo(output)
                    }
                }

                zipIn.closeEntry()

                entry = zipIn.nextEntry
            }
        }

        //Reading Manifest
        val manifestFile = File(tempDir, "manifest.json")
        val manifest = Json.decodeFromString<TransferManifest>(
            manifestFile.readText()
        )
        return manifest to tempDir
    }

    private fun createTempExportDir(): File {
        val dir = File(context.cacheDir, "export_temp")

        if (dir.exists()) {
            dir.deleteRecursively()
        }

        dir.mkdirs()

        return dir
    }

    private fun copyUriToFile(
        uri: Uri,
        destination: File
    ) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun zipDirectory(
        sourceDir: File,
        outputStream: OutputStream
    ) {

        ZipOutputStream(
            BufferedOutputStream(outputStream)
        ).use { zipOut ->

            sourceDir.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->

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