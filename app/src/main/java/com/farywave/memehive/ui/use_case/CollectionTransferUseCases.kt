package com.farywave.memehive.ui.use_case

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.farywave.memehive.core.collection_transfer.CollectionArchiveReader
import com.farywave.memehive.core.collection_transfer.entity.TransferCollection
import com.farywave.memehive.core.collection_transfer.entity.TransferManifest
import com.farywave.memehive.core.collection_transfer.entity.TransferMediaItem
import com.farywave.memehive.core.collection_transfer.mapper.toCollection
import com.farywave.memehive.core.collection_transfer.mapper.toMediaItem
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.data.local.db.repository.ImageRepository
import com.farywave.memehive.domain.model.Collection
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files

class CollectionTransferUseCases(
    private val db: MemeHiveDatabase,
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
    private val collectionUseCases: CollectionUseCases,
    private val mediaItemUseCases: MediaItemUseCases,
    private val imageRepository: ImageRepository,
) {
    private val json = Json {
        prettyPrint = true
    }

    suspend fun importCollection(uri: Uri) {
        withTransferDirectory("import_") { dir ->
            val manifest = unzipCollection(uri, dir)
            imageRepository.withImageChanges { changes ->
                db.withTransaction {
                    val collection = collectionUseCases.createCollection(
                        collection = manifest.collection.toCollection(
                            cover = manifest.collection.coverPath?.let {
                                File(
                                    dir,
                                    it
                                ).toUri()
                            }
                        ),
                        changes = changes,
                    )

                    val mediaItems = manifest.mediaItems.map { transferMediaItem ->
                        mediaItemUseCases.createMediaItem(
                            mediaItem = transferMediaItem.toMediaItem(
                                image = transferMediaItem.path?.let { File(dir, it).toUri() }
                            ),
                            changes = changes,
                        )
                    }

                    collectionUseCases.addItemsToCollection(
                        mediaItems.map { it.id }.toSet(),
                        collection.id
                    )
                }
            }
        }
    }

    suspend fun exportCollection(
        collection: Collection,
        outputUri: Uri
    ) {
        withTransferDirectory("export_") { dir ->
            val mediaItems = mediaItemUseCases.getByCollectionId(collection.id)
            val collectionDir = File(dir, "collection")
            val mediaDir = File(dir, "media")

            val collectionCoverPath =
                "collection/cover.${
                    collection.cover?.uri?.lastPathSegment?.substringAfterLast(
                        '.',
                        ""
                    )?.takeIf { it.isNotBlank() } ?: "bin"
                }"

            createDirectory(collectionDir)
            createDirectory(mediaDir)

            //Collection Preparation

            collection.cover?.let { cover ->

                val coverUri = cover.uri

                copyUriToFile(
                    coverUri,
                    File(
                        dir,
                        collectionCoverPath
                    )
                )
            }

            //Media Preparation
            val transferMediaItems =
                mediaItems.filter { it.image != null }.mapIndexed { index, item ->
                    val image = item.image!!.uri
                    val extension =
                        image.lastPathSegment?.substringAfterLast('.', "bin") ?: "bin"

                    val exportName = "%03d.%s".format(index, extension)

                    val relativePath = "media/$exportName"
                    copyUriToFile(
                        image,
                        File(dir, relativePath)
                    )

                    TransferMediaItem(
                        path = relativePath,
                        caption = item.caption,
                        description = item.description,
                        tags = item.tags
                    )
                }

            //Manifest Preparation
            val manifest = TransferManifest(
                appVersion = context.packageManager.getPackageInfo(
                    context.packageName,
                    0
                ).versionCode,
                exportedAt = System.currentTimeMillis(),
                collection = TransferCollection(
                    name = collection.name,
                    coverPath = collection.cover?.let {
                        collectionCoverPath
                    }
                ),
                mediaItems = transferMediaItems
            )

            //Manifest Serialization
            val manifestFile = File(dir, "manifest.json")

            manifestFile.writeText(
                json.encodeToString(manifest)
            )

            // Zip the directory
            val output = context.contentResolver.openOutputStream(outputUri)
                ?: throw IOException("Could not open the export destination.")
            output.use { zipDirectory(dir, it) }
        }
    }

    private suspend fun unzipCollection(uri: Uri, directory: File): TransferManifest =
        withContext(ioDispatcher) {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Could not open the collection archive.")
            CollectionArchiveReader.extract(input, directory)
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

    private suspend fun <T> withTransferDirectory(
        prefix: String,
        block: suspend (File) -> T,
    ): T = withContext(ioDispatcher) {
        val directory = Files.createTempDirectory(
            context.cacheDir.toPath(),
            prefix,
        ).toFile()

        try {
            block(directory)
        } finally {
            withContext(NonCancellable) {
                try {
                    if (!directory.deleteRecursively()) {
                        Log.w("CollectionTransfer", "Could not delete $directory")
                    }
                } catch (error: Exception) {
                    Log.w("CollectionTransfer", "Cleanup failed", error)
                }
            }
        }
    }
}
