package com.farywave.memehive.data.local.storage

import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.domain.model.aspectRatio
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageDimensionsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val storage = ImageStorage(context, Dispatchers.IO)

    @Test fun readsDimensionsAndHonorsRotation(): Unit = runBlocking {
        val file = File.createTempFile("dimensions", ".jpg", context.cacheDir)
        try {
            val bitmap = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            bitmap.recycle()
            assertEquals(80 to 40, storage.readDimensions(file))
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }
            assertEquals(40 to 80, storage.readDimensions(file))
        } finally {
            file.delete()
        }
    }

    @Test fun unreadableDimensionsUseFallback(): Unit = runBlocking {
        val file = File.createTempFile("dimensions", ".bin", context.cacheDir)
        try {
            file.writeText("not an image")
            assertNull(storage.readDimensions(file))
        } finally {
            file.delete()
        }
        assertEquals(1.5f, (null as Image?).aspectRatio)
        for ((width, height) in listOf(null to null, 80 to null, null to 40, 0 to 40, 80 to -1)) {
            assertEquals(1.5f, Image(uri = Uri.EMPTY, width = width, height = height).aspectRatio)
        }
        assertEquals(2f, Image(uri = Uri.EMPTY, width = 80, height = 40).aspectRatio)
    }
}
