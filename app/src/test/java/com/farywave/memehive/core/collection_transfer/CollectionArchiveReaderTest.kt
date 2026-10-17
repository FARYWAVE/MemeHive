package com.farywave.memehive.core.collection_transfer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CollectionArchiveReaderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val manifest = """
        {
          "appVersion": 1,
          "exportedAt": 0,
          "collection": {"name": "Saved memes", "coverPath": "collection/cover.jpg"},
          "mediaItems": [
            {"path": "media/000.jpg", "caption": "Caption", "description": "", "tags": []},
            {"path": null, "caption": "Text only", "description": "", "tags": []}
          ]
        }
    """.trimIndent()

    @Test
    fun `valid archive extracts images and reads the complete manifest`() = runTest {
        val directory = temporaryFolder.newFolder()
        val result = CollectionArchiveReader.extract(validArchive().inputStream(), directory)

        assertEquals("Saved memes", result.collection.name)
        assertEquals(2, result.mediaItems.size)
        assertEquals("image bytes", File(directory, "media/000.jpg").readText())
        assertEquals("cover bytes", File(directory, "collection/cover.jpg").readText())
    }

    @Test
    fun `non zip input is an invalid archive`() = runTest {
        expectFailure<InvalidCollectionArchiveException> {
            CollectionArchiveReader.extract("not a zip".byteInputStream(), temporaryFolder.newFolder())
        }
    }

    @Test
    fun `missing manifest is an invalid archive`() = runTest {
        expectFailure<InvalidCollectionArchiveException> {
            CollectionArchiveReader.extract(
                zipOf("unrelated.txt" to "hello").inputStream(),
                temporaryFolder.newFolder(),
            )
        }
    }

    @Test
    fun `malformed manifest preserves the parsing cause`() = runTest {
        val failure = expectFailure<InvalidCollectionArchiveException> {
            CollectionArchiveReader.extract(
                zipOf("manifest.json" to "{broken json").inputStream(),
                temporaryFolder.newFolder(),
            )
        }
        assertNotNull(failure.cause)
    }

    @Test
    fun `missing and empty images are rejected before saving the collection`() = runTest {
        for (imageEntries in listOf(emptyList(), listOf("media/000.jpg" to ""))) {
            val archive = zipOf(
                "manifest.json" to manifest,
                "collection/cover.jpg" to "cover bytes",
                *imageEntries.toTypedArray(),
            )
            expectFailure<InvalidCollectionArchiveException> {
                CollectionArchiveReader.extract(archive.inputStream(), temporaryFolder.newFolder())
            }
        }
    }

    @Test
    fun `zip entry cannot escape to a sibling with the same directory prefix`() = runTest {
        val directory = temporaryFolder.newFolder("import")
        expectFailure<InvalidCollectionArchiveException> {
            CollectionArchiveReader.extract(
                zipOf("../import_other/stolen.jpg" to "image bytes").inputStream(),
                directory,
            )
        }
        assertFalse(File(temporaryFolder.root, "import_other/stolen.jpg").exists())
    }

    @Test
    fun `manifest cannot reference an image outside the archive`() = runTest {
        val outside = temporaryFolder.newFile("outside.jpg").apply { writeText("existing image") }
        val archive = zipOf(
            "manifest.json" to manifest.replace("media/000.jpg", "../outside.jpg"),
            "collection/cover.jpg" to "cover bytes",
        )
        expectFailure<InvalidCollectionArchiveException> {
            CollectionArchiveReader.extract(archive.inputStream(), temporaryFolder.newFolder())
        }
        assertEquals("existing image", outside.readText())
    }

    @Test
    fun `truncated zip preserves the underlying failure`() = runTest {
        val failure = expectFailure<InvalidCollectionArchiveException> {
            CollectionArchiveReader.extract(
                validArchive().copyOf(50).inputStream(),
                temporaryFolder.newFolder(),
            )
        }
        assertNotNull(failure.cause)
    }

    @Test
    fun `file read errors propagate unchanged and close the stream`() = runTest {
        val error = IOException("Provider unavailable")
        val input = FailingInputStream(error)
        val failure = expectFailure<IOException> {
            CollectionArchiveReader.extract(input, temporaryFolder.newFolder())
        }
        assertSame(error, failure)
        assertTrue(input.closed)
    }

    @Test
    fun `cancellation propagates unchanged and closes the stream`() = runTest {
        val cancelled = CancellationException("Import cancelled")
        val input = FailingInputStream(cancelled)
        val failure = expectFailure<CancellationException> {
            CollectionArchiveReader.extract(input, temporaryFolder.newFolder())
        }
        assertSame(cancelled, failure)
        assertTrue(input.closed)
    }

    private fun validArchive() = zipOf(
        "manifest.json" to manifest,
        "collection/cover.jpg" to "cover bytes",
        "media/000.jpg" to "image bytes",
    )

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            for ((name, contents) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(contents.toByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private class FailingInputStream(private val failure: Exception) : InputStream() {
        var closed = false
        override fun read(): Int = throw failure
        override fun close() { closed = true }
    }

    private suspend inline fun <reified T : Throwable> expectFailure(block: suspend () -> Unit): T {
        try {
            block()
        } catch (failure: Throwable) {
            if (failure is T) return failure
            throw failure
        }
        throw AssertionError("Expected ${T::class.simpleName}")
    }
}
