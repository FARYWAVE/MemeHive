package com.farywave.memehive.core

import com.farywave.memehive.data.local.storage.ImageStorage
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

class DeviceToolsTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val ioThread = AtomicReference<Thread>()
    private val ioDispatcher = Executors.newSingleThreadExecutor { task ->
        Thread(task, "storage-test-io").also { ioThread.set(it) }
    }.asCoroutineDispatcher()

    @After
    fun closeDispatcher() {
        ioDispatcher.close()
    }

    @Test
    fun `directory size scans nested files on the supplied dispatcher`() = runBlocking {
        val directory = temporaryFolder.newFolder("storage")
        val nested = File(directory, "nested").apply { mkdir() }
        File(directory, "first.bin").writeBytes(ByteArray(5))
        File(nested, "second.bin").writeBytes(ByteArray(7))
        val callerThread = Thread.currentThread()
        var scanThread: Thread? = null
        val observedDirectory = object : File(directory.path) {
            override fun listFiles(): Array<File>? {
                scanThread = Thread.currentThread()
                return super.listFiles()
            }
        }

        val size = DeviceTools.dirSize(observedDirectory, ioDispatcher)

        assertEquals(directory.length() + nested.length() + 12L, size)
        assertSame(ioThread.get(), scanThread)
        assertNotSame(callerThread, scanThread)
        assertSame(callerThread, Thread.currentThread())
    }

    @Test
    fun `deletion runs on the supplied dispatcher and resumes the caller`() = runBlocking {
        val file = temporaryFolder.newFile("unused-image")
        val callerThread = Thread.currentThread()
        var deletionThread: Thread? = null
        val observedFile = object : File(file.path) {
            override fun delete(): Boolean {
                deletionThread = Thread.currentThread()
                return super.delete()
            }
        }

        ImageStorage.deleteFile(observedFile, ioDispatcher)

        assertFalse(file.exists())
        assertSame(ioThread.get(), deletionThread)
        assertNotSame(callerThread, deletionThread)
        assertSame(callerThread, Thread.currentThread())
    }
}
