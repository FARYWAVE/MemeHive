package com.farywave.memehive.data.local.storage

import android.content.ClipData
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ImageSharingTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val storage = ImageStorage(context, Dispatchers.IO)

    @Test fun sharedUriIsReadableAndHasImageMimeType(): Unit = runBlocking {
        val directory = File(context.filesDir, "images").apply { mkdirs() }
        val file = File.createTempFile("sharing_", ".png", directory)
        try {
            val bytes = byteArrayOf(1, 2, 3, 4)
            file.writeBytes(bytes)
            val uri = storage.sharingUri(file.name)
            assertEquals("content", uri.scheme)
            assertEquals("${context.packageName}.provider", uri.authority)
            assertEquals("image/png", context.contentResolver.getType(uri))
            assertArrayEquals(bytes, context.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
            val clip = ClipData.newUri(context.contentResolver, "Image", uri)
            assertEquals(uri, clip.getItemAt(0).uri)
            assertTrue(clip.description.hasMimeType("image/png"))
        } finally {
            file.delete()
        }
    }

    @Test fun providerRejectsFilesOutsideImageDirectory() {
        val file = File.createTempFile("private_", ".txt", context.filesDir)
        try {
            assertThrows(IllegalArgumentException::class.java) {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            }
        } finally {
            file.delete()
        }
    }

    @Test fun missingImagesFailBeforeClipboardCopy(): Unit = runBlocking {
        val directory = File(context.filesDir, "images").apply { mkdirs() }
        val file = File.createTempFile("missing_", ".png", directory)
        check(file.delete())
        try {
            storage.sharingUri(file.name)
            fail("Expected missing image failure")
        } catch (_: IOException) {
            // The picker can show the failure without replacing the clipboard.
        }
    }
}
