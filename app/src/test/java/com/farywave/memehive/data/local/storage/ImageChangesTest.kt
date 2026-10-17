package com.farywave.memehive.data.local.storage

import com.farywave.memehive.core.DeviceTools
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.io.InputStream

class ImageChangesTest {
    @get:Rule
    val directory = TemporaryFolder()

    @Test
    fun `successful operation preserves saved images and uses unique file names`() = runTest {
        val storedPaths = mutableSetOf<String>()
        val files = withChanges(storedPaths) { changes ->
            val first = copy(changes, "first".byteInputStream())
            val second = copy(changes, "second".byteInputStream())
            storedPaths.addAll(listOf(first.path, second.path))
            first to second
        }

        assertNotEquals(files.first.path, files.second.path)
        assertEquals("first", files.first.readText())
        assertEquals("second", files.second.readText())
    }

    @Test
    fun `rollback removes this operations new files and leaves existing images alone`() = runTest {
        val existing = directory.newFile("existing.jpg").apply { writeText("saved") }
        val storedPaths = mutableSetOf(existing.path)
        val newFiles = mutableListOf<File>()
        val originalFailure = IOException("Failed to save tags")

        val failure = expectFailure<IOException> {
            withChanges(storedPaths) { changes ->
                newFiles += copy(changes, "cover".byteInputStream())
                newFiles += copy(changes, "media item".byteInputStream())
                // A rolled-back transaction leaves no database rows for either new file.
                throw originalFailure
            }
        }

        assertSame(originalFailure, failure)
        assertTrue(newFiles.none { it.exists() })
        assertEquals("saved", existing.readText())
    }

    @Test
    fun `one failed operation does not delete another operations uncommitted file`() = runTest {
        val storedPaths = mutableSetOf<String>()
        val otherFileReady = CompletableDeferred<File>()
        val allowOtherCommit = CompletableDeferred<Unit>()
        val otherOperation = async {
            withChanges(storedPaths) { changes ->
                val file = copy(changes, "other operation".byteInputStream())
                otherFileReady.complete(file)
                allowOtherCommit.await()
                storedPaths.add(file.path)
            }
        }
        val otherFile = otherFileReady.await()
        lateinit var abandoned: File

        expectFailure<IOException> {
            withChanges(storedPaths) { changes ->
                abandoned = copy(changes, "failed operation".byteInputStream())
                throw IOException("Save failed")
            }
        }

        assertFalse(abandoned.exists())
        assertTrue(otherFile.exists())
        assertFalse(otherFile.path in storedPaths)
        allowOtherCommit.complete(Unit)
        otherOperation.await()
        assertEquals("other operation", otherFile.readText())
    }

    @Test
    fun `partial copy is recorded and removed when reading fails`() = runTest {
        val originalFailure = IOException("Image provider disconnected")
        lateinit var partialFile: File
        var closed = false
        val input = object : InputStream() {
            var hasRead = false
            override fun read(): Int = error("Bulk reads expected")
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (hasRead) throw originalFailure
                hasRead = true
                buffer[offset] = 1
                return 1
            }
            override fun close() { closed = true }
        }

        val failure = expectFailure<IOException> {
            withChanges { changes ->
                DeviceTools.copyImageToDirectory(
                    directory.root, "jpg", StandardTestDispatcher(testScheduler),
                    onFileCreated = {
                        partialFile = it
                        changes.recordCreated(it)
                    },
                    openInput = { input },
                )
            }
        }

        // Coroutine stack-trace recovery may copy the exception, retaining it as the cause.
        assertEquals(originalFailure.message, failure.message)
        assertTrue(generateSequence<Throwable>(failure) { it.cause }.any { it === originalFailure })
        assertFalse(partialFile.exists())
        assertTrue(closed)
    }

    @Test
    fun `cancellation before the copy result is delivered still cleans the recorded file`() = runTest {
        lateinit var copiedFile: File
        var returnedFromCopy = false
        val operation = launch {
            val operationJob = currentCoroutineContext().job
            val bytes = "image".byteInputStream()
            val input = object : InputStream() {
                override fun read(): Int = error("Bulk reads expected")
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                    val count = bytes.read(buffer, offset, length)
                    if (count == -1) operationJob.cancel()
                    return count
                }
            }
            withChanges { changes ->
                DeviceTools.copyImageToDirectory(
                    directory.root, "jpg", StandardTestDispatcher(testScheduler),
                    onFileCreated = {
                        copiedFile = it
                        changes.recordCreated(it)
                    },
                    openInput = { input },
                )
                returnedFromCopy = true
            }
        }
        operation.join()

        assertTrue(operation.isCancelled)
        assertFalse(returnedFromCopy)
        assertFalse(copiedFile.exists())
    }

    @Test
    fun `cancellation after the database commit preserves the saved file`() = runTest {
        val storedPaths = mutableSetOf<String>()
        val committedFile = CompletableDeferred<File>()
        val operation = launch {
            withChanges(storedPaths) { changes ->
                val file = copy(changes, "saved image".byteInputStream())
                storedPaths.add(file.path)
                committedFile.complete(file)
                awaitCancellation()
            }
        }
        val file = committedFile.await()
        operation.cancelAndJoin()

        assertTrue(operation.isCancelled)
        assertEquals("saved image", file.readText())
    }

    @Test
    fun `failed database check keeps the file without hiding the operation failure`() = runTest {
        val file = directory.newFile("unknown.jpg")
        val originalFailure = IOException("Original failure")
        val databaseFailure = IOException("Cannot check the database")
        val cleanupFailures = mutableListOf<Exception>()

        val failure = expectFailure<IOException> {
            ImageChanges.track(
                ioDispatcher = StandardTestDispatcher(testScheduler),
                isStored = { throw databaseFailure },
                onCleanupFailure = { _, error -> cleanupFailures += error },
            ) { changes ->
                changes.recordCreated(file)
                throw originalFailure
            }
        }

        assertSame(originalFailure, failure)
        assertEquals(listOf(databaseFailure), cleanupFailures)
        assertTrue(file.exists())
    }

    @Test
    fun `failed deletion is reported and remaining files are still cleaned`() = runTest {
        val realFile = directory.newFile("undeletable.jpg")
        val undeletable = object : File(realFile.path) {
            override fun delete(): Boolean = false
        }
        val abandoned = directory.newFile("abandoned.jpg")
        val cleanupFailures = mutableListOf<Exception>()

        val result = withChanges(cleanupFailures = cleanupFailures) { changes ->
            changes.recordCreated(undeletable)
            changes.recordCreated(abandoned)
            "result"
        }

        assertEquals("result", result)
        assertEquals(1, cleanupFailures.size)
        assertTrue(realFile.exists())
        assertFalse(abandoned.exists())
    }

    private suspend fun <T> TestScope.withChanges(
        storedPaths: Set<String> = emptySet(),
        cleanupFailures: MutableList<Exception> = mutableListOf(),
        block: suspend (ImageChanges) -> T,
    ): T = ImageChanges.track(
        ioDispatcher = StandardTestDispatcher(testScheduler),
        isStored = { path ->
            yield() // The real DAO lookup suspends, including during cancellation cleanup.
            path in storedPaths
        },
        onCleanupFailure = { _, error -> cleanupFailures += error },
        block = block,
    )

    private suspend fun TestScope.copy(changes: ImageChanges, input: InputStream): File =
        DeviceTools.copyImageToDirectory(
            directory.root, "jpg", StandardTestDispatcher(testScheduler),
            onFileCreated = changes::recordCreated,
            openInput = { input },
        )

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
