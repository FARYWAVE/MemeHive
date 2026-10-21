package com.farywave.memehive.core.collection_transfer

import com.farywave.memehive.core.collection_transfer.entity.TransferManifest
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

internal object CollectionArchiveReader {
    suspend fun extract(input: InputStream, directory: File): TransferManifest {
        try {
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    currentCoroutineContext().ensureActive()
                    val destination = resolvePath(directory, entry.name)
                    if (entry.isDirectory) {
                        createDirectory(destination)
                    } else {
                        createDirectory(destination.parentFile!!)
                        destination.outputStream().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val count = zip.read(buffer)
                                if (count == -1) break
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (error: ZipException) {
            throw InvalidCollectionArchiveException("The ZIP archive is corrupt.", error)
        } catch (error: EOFException) {
            throw InvalidCollectionArchiveException("The ZIP archive is incomplete.", error)
        }

        val manifestFile = File(directory, "manifest.json")
        if (!manifestFile.isFile) {
            throw InvalidCollectionArchiveException("The archive has no manifest.json file.")
        }
        val manifest = try {
            Json.decodeFromString<TransferManifest>(manifestFile.readText())
        } catch (error: SerializationException) {
            throw InvalidCollectionArchiveException("The collection manifest is invalid.", error)
        }

        val paths = listOfNotNull(manifest.collection.coverPath) +
            manifest.mediaItems.mapNotNull { it.path }
        paths.forEach { path ->
            currentCoroutineContext().ensureActive()
            val file = resolvePath(directory, path)
            if (!file.isFile || file.length() == 0L) {
                throw InvalidCollectionArchiveException("An image is missing or empty: $path")
            }
        }
        return manifest
    }

    private fun resolvePath(directory: File, path: String): File {
        if (path.isBlank() || File(path).isAbsolute) {
            throw InvalidCollectionArchiveException("Invalid archive path: $path")
        }
        val file = File(directory, path).canonicalFile
        val root = directory.canonicalPath + File.separator
        if (!file.path.startsWith(root)) {
            throw InvalidCollectionArchiveException("Archive path escapes its directory: $path")
        }
        return file
    }

    private fun createDirectory(directory: File) {
        if (!directory.mkdirs() && !directory.isDirectory) {
            throw IOException("Could not create directory: $directory")
        }
    }
}
