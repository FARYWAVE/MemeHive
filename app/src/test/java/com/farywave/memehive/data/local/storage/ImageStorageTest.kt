package com.farywave.memehive.data.local.storage

import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImageStorageTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun `resolver accepts filenames but rejects paths`() {
        val root = temporary.root
        assertEquals(
            File(root, "мем with spaces.png"),
            ImageStorage.resolve(root, "мем with spaces.png"),
        )
        for (name in listOf("", " ", ".", "..", "../image.png", "/tmp/image.png", "a/b", "a\\b")) {
            assertThrows(IllegalArgumentException::class.java) { ImageStorage.resolve(root, name) }
        }
    }

    @Test
    fun `copy creates the directory and can recreate it after deletion`() = runTest {
        val directory = File(temporary.root, "images")
        repeat(2) {
            val file =
                ImageStorage.copyImageToDirectory(
                    directory,
                    "png",
                    StandardTestDispatcher(testScheduler),
                ) {
                    "image".byteInputStream()
                }
            assertEquals("image", file.readText())
            assertEquals(directory, file.parentFile)
            assertTrue(directory.deleteRecursively())
        }
    }

    @Test
    fun `copy rejects a regular file occupying the directory`() = runTest {
        val directory = temporary.newFile("images")
        try {
            ImageStorage.copyImageToDirectory(
                directory,
                "png",
                StandardTestDispatcher(testScheduler),
            ) {
                "image".byteInputStream()
            }
            fail("Expected failure")
        } catch (_: IOException) {
            assertTrue(directory.isFile)
        }
    }

    @Test
    fun `failed copy without a tracker removes its partial file`() = runTest {
        val directory = temporary.newFolder("images")
        val input =
            object : InputStream() {
                override fun read(): Int = throw IOException("Provider disconnected")
            }
        try {
            ImageStorage.copyImageToDirectory(
                directory,
                "png",
                StandardTestDispatcher(testScheduler),
            ) {
                input
            }
            fail("Expected failure")
        } catch (_: IOException) {
            assertTrue(directory.listFiles()!!.isEmpty())
        }
    }
}
