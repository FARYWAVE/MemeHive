package com.farywave.memehive.data.local.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import android.webkit.MimeTypeMap
import com.farywave.memehive.core.DeviceTools.getFileName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream

class ImageStorage(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val context = context.applicationContext

    fun resolve(imageName: String): File = resolve(File(context.filesDir, "images"), imageName)

    suspend fun sharingUri(imageName: String): Uri = withContext(ioDispatcher) {
        val file = resolve(imageName)
        if (!file.isFile || !file.canRead()) throw IOException("Image is unavailable: $imageName")
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    suspend fun copy(
        uri: Uri,
        onFileCreated: (File) -> Unit = {},
    ): File {
        val resolver = context.contentResolver
        val extension = withContext(ioDispatcher) {
            val mimeType = resolver.getType(uri)?.substringBefore(';')?.lowercase()
            val extFromMime = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            val displayName = if (uri.scheme == "file") uri.lastPathSegment else getFileName(context, uri, ioDispatcher)
            val extFromName = displayName?.substringAfterLast('.', "")?.lowercase()
            listOfNotNull(extFromMime, extFromName)
                .firstOrNull { it.matches(Regex("[a-z0-9]{1,10}")) } ?: "bin"
        }

        return copyImageToDirectory(File(context.filesDir, "images"), extension, ioDispatcher, onFileCreated) {
            resolver.openInputStream(uri) ?: throw IOException("Could not open the source image.")
        }
    }

    suspend fun delete(imageName: String) = deleteFile(resolve(imageName), ioDispatcher)

    internal companion object {
        fun resolve(directory: File, name: String): File {
            require(name.isNotBlank() && name != "." && name != ".." &&
                '/' !in name && '\\' !in name && '\u0000' !in name) {
                "Expected an image filename, not a path"
            }
            return File(directory, name)
        }

        suspend fun deleteFile(file: File, ioDispatcher: CoroutineDispatcher) {
            withContext(ioDispatcher) {
                if (!file.delete() && file.exists()) throw IOException("Could not delete image: $file")
            }
        }

        internal suspend fun copyImageToDirectory(
            directory: File,
            extension: String,
            ioDispatcher: CoroutineDispatcher,
            onFileCreated: (File) -> Unit = {},
            openInput: () -> InputStream,
        ): File {
            var created: File? = null
            try {
                return withContext(ioDispatcher) {
                    if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
                        throw IOException("Could not create image directory: $directory")
                    }
                    require(extension.matches(Regex("[a-zA-Z0-9]{1,10}"))) { "Invalid image extension" }
                    openInput().use { input ->
                        val file = File.createTempFile("image_", ".$extension", directory)
                        created = file
                        // Register before returning: cancellation can discard a withContext result.
                        onFileCreated(file)
                        file.outputStream().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                ensureActive()
                                val count = input.read(buffer)
                                if (count == -1) break
                                output.write(buffer, 0, count)
                            }
                        }
                        if (file.length() == 0L) throw IOException("Failed to copy file")
                        file
                    }
                }
            } catch (error: Throwable) {
                withContext(NonCancellable + ioDispatcher) {
                    created?.let { file ->
                        if (!file.delete() && file.exists()) {
                            error.addSuppressed(IOException("Could not remove partial image: $file"))
                        }
                    }
                }
                throw error
            }
        }
    }
}
