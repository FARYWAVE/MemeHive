package com.farywave.memehive.application.usecase

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.farywave.memehive.R
import com.farywave.memehive.core.collection_transfer.InvalidCollectionArchiveException
import com.farywave.memehive.core.collection_transfer.entity.TransferCollection
import com.farywave.memehive.core.collection_transfer.entity.TransferManifest
import com.farywave.memehive.core.collection_transfer.entity.TransferMediaItem
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.data.local.db.entity.ImageEntity
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.ImageRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.data.local.db.repository.TagRepository
import com.farywave.memehive.data.local.storage.ImageStorage
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.domain.model.MediaItem
import com.farywave.memehive.ui.activity.main.screen.editing.EditingViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionTransferUseCasesTest {
    private lateinit var root: File
    private lateinit var context: Context
    private lateinit var db: MemeHiveDatabase
    private lateinit var images: ImageRepository
    private lateinit var collections: CollectionUseCases
    private lateinit var media: MediaItemUseCases
    private lateinit var transfer: CollectionTransferUseCases

    @Before
    fun setUp() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        root = Files.createTempDirectory(base.cacheDir.toPath(), "transfer_test_").toFile()
        context =
            object : ContextWrapper(base) {
                override fun getApplicationContext(): Context = this

                override fun getFilesDir(): File = File(root, "files").apply { mkdirs() }

                override fun getCacheDir(): File = File(root, "cache").apply { mkdirs() }
            }
        db = Room.inMemoryDatabaseBuilder(base, MemeHiveDatabase::class.java).build()
        images =
            ImageRepository(
                context,
                db.imageDao(),
                Dispatchers.IO,
                ImageStorage(context, Dispatchers.IO),
            )
        val collectionRepository =
            CollectionRepository(db.collectionDao(), images::toModel, db.collectionEntryDao())
        val deletionMediaRepo = MediaItemRepository(db.mediaItemDao(), images::toModel)
        val tagRepository = TagRepository(db.tagDao(), db.mediaItemTagDao())
        val deleteMediaItems = DeleteMediaItemsUseCase(db, deletionMediaRepo, tagRepository, images)
        collections = CollectionUseCases(db, collectionRepository, images, deleteMediaItems)
        media =
            MediaItemUseCases(
                db,
                deletionMediaRepo,
                tagRepository,
                collectionRepository,
                images,
                deleteMediaItems,
            )
        transfer =
            CollectionTransferUseCases(db, context, Dispatchers.IO, collections, media, images)
    }

    @After
    fun tearDown() {
        db.close()
        root.deleteRecursively()
    }

    @Test
    fun textOnlyItemsSurviveExportAndImport(): Unit = runBlocking {
        val original = collections.createCollection(Collection(name = "Text collection"))
        media.createMediaItem(
            MediaItem(caption = "No image", description = "Keep me", tags = listOf("text")),
            setOf(original.id),
        )
        val archive = File(root, "text.zip")
        transfer.exportCollection(original, archive.toUri())
        transfer.importCollection(archive.toUri())
        val imported = collections.observeCollections().first().single { it.id != original.id }
        val item = media.getByCollectionId(imported.id).single()
        assertEquals("No image", item.caption)
        assertEquals("Keep me", item.description)
        assertEquals(listOf("text"), item.tags)
        assertNull(item.image)
    }

    @Test
    fun editorClosesOnlyAfterSaveAndPreservesDraftOnFailure(): Unit = runBlocking {
        val stores = mutableListOf<ViewModelStore>()
        suspend fun editor(): EditingViewModel =
            withContext(Dispatchers.Main) {
                EditingViewModel(0, media, collections, SavedStateHandle()).also {
                    stores += ViewModelStore().apply { put("editor", it) }
                }
            }
        try {
            val successful = editor()
            withTimeout(5_000.milliseconds) { successful.isLoading.first { !it } }
            withContext(Dispatchers.Main) {
                successful.updateCaption("Saved before closing")
                successful.saveCurrentItem(closeAfterSave = true)
                assertFalse(successful.closeRequested.value)
            }
            withTimeout(5_000.milliseconds) { successful.closeRequested.first { it } }
            assertEquals(
                "Saved before closing",
                media.getMediaItemById(successful.mediaItemDraft.value.id)?.caption,
            )

            val failed = editor()
            withTimeout(5_000.milliseconds) { failed.isLoading.first { !it } }
            db.close()
            withContext(Dispatchers.Main) {
                failed.updateCaption("Keep this draft")
                failed.saveCurrentItem(closeAfterSave = true)
            }
            val error = withTimeout(5_000.milliseconds) { failed.error.first { it != null } }
            assertEquals(R.string.error_media_save, error!!.messageResId)
            assertFalse(failed.closeRequested.value)
            assertEquals("Keep this draft", failed.mediaItemDraft.value.caption)
            withTimeout(5_000.milliseconds) { failed.isSaving.first { !it } }
            assertFalse(failed.isSaving.value)
        } finally {
            withContext(Dispatchers.Main) { stores.forEach { it.clear() } }
        }
    }

    @Test
    fun editorReportsMissingItemInsteadOfCreatingBlankDraft(): Unit = runBlocking {
        val store = ViewModelStore()
        try {
            val editor =
                withContext(Dispatchers.Main) {
                    EditingViewModel(999, media, collections, SavedStateHandle()).also {
                        store.put("editor", it)
                    }
                }
            withTimeout(5_000.milliseconds) { editor.isLoading.first { !it } }
            assertEquals(R.string.error_media_missing, editor.loadError.value)
            assertEquals(999L, editor.mediaItemDraft.value.id)
            withContext(Dispatchers.Main) { editor.saveCurrentItem(closeAfterSave = true) }
            assertFalse(editor.closeRequested.value)
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
        }
    }

    @Test
    fun storedFilenameWithSpecialCharactersResolvesToOriginalBytes(): Unit = runBlocking {
        val name = "мем with spaces #100%.png"
        val directory = File(context.filesDir, "images").apply { mkdirs() }
        val bytes = png()
        File(directory, name).writeBytes(bytes)
        val id = db.imageDao().insert(ImageEntity(name = name, sha256 = "fixture"))
        val model = images.toModel(db.imageDao().getById(id)!!)
        assertEquals("file", model.uri.scheme)
        assertEquals(name, model.uri.lastPathSegment)
        assertArrayEquals(bytes, read(model.uri))
        assertEquals(name, db.imageDao().getById(id)!!.name)
    }

    @Test
    fun pngAndWebpCoversRoundTripWithMatchingManifestAndUnchangedBytes(): Unit = runBlocking {
        // A fixture exercises WebP on API 29 too, without the deprecated encoder.
        val webp =
            InstrumentationRegistry.getInstrumentation().context.assets.open("blue.webp").use {
                it.readBytes()
            }
        for ((extension, bytes) in listOf("png" to png(), "webp" to webp)) {
            val source = File(root, "source.$extension").apply { writeBytes(bytes) }
            val original =
                collections.createCollection(
                    Collection(name = extension, cover = Image(uri = source.toUri()))
                )
            media.createMediaItem(
                MediaItem(
                    image = Image(uri = source.toUri()),
                    caption = "caption",
                    tags = listOf("tag"),
                ),
                setOf(original.id),
            )
            val archive = File(root, "$extension.zip")
            transfer.exportCollection(original, archive.toUri())
            assertNoTransferDirectories()
            ZipFile(archive).use { zip ->
                val manifest =
                    Json.decodeFromString<TransferManifest>(
                        zip.getInputStream(zip.getEntry("manifest.json")).bufferedReader().use {
                            it.readText()
                        }
                    )
                assertEquals("collection/cover.$extension", manifest.collection.coverPath)
                assertArrayEquals(
                    bytes,
                    zip.getInputStream(zip.getEntry(manifest.collection.coverPath)).use {
                        it.readBytes()
                    },
                )
            }
            transfer.importCollection(archive.toUri())
            val imported =
                collections.observeCollections().first().single {
                    it.name == extension && it.id != original.id
                }
            assertArrayEquals(bytes, read(imported.cover!!.uri))
            val item = media.getByCollectionId(imported.id).single()
            assertEquals("caption", item.caption)
            assertEquals(listOf("tag"), item.tags)
            assertArrayEquals(bytes, read(item.image!!.uri))
            assertNoTransferDirectories()
        }
    }

    @Test
    fun invalidArchiveRemovesExtractedFilesAndDoesNotWriteDatabase(): Unit = runBlocking {
        val archive = File(root, "invalid.zip")
        ZipOutputStream(archive.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("partial.png"))
            zip.write(png())
            zip.closeEntry()
        }
        expectFailure<InvalidCollectionArchiveException> {
            transfer.importCollection(archive.toUri())
        }
        assertEmptyDatabaseAndImages()
        assertNoTransferDirectories()
    }

    @Test
    fun transactionFailureRemovesCopiedImagesAndAllImportedRecords(): Unit = runBlocking {
        // Fail after the cover, item, image, tags and search entries have been inserted.
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_membership BEFORE INSERT ON collection_entries
            BEGIN SELECT RAISE(ABORT, 'forced membership failure'); END
            """
                .trimIndent()
        )
        expectFailure<android.database.sqlite.SQLiteException> {
            transfer.importCollection(archive("rollback").toUri())
        }
        assertEmptyDatabaseAndImages()
        assertNoTransferDirectories()
    }

    @Test
    fun failedExportDestinationStillRemovesTemporaryDirectory(): Unit = runBlocking {
        val collection = collections.createCollection(Collection(name = "export"))
        val destination = File(root, "destination-directory").apply { mkdirs() }
        expectFailure<IOException> { transfer.exportCollection(collection, destination.toUri()) }
        assertNoTransferDirectories()
        assertEquals(collection.id, collections.observeCollections().first().single().id)
    }

    @Test
    fun cancellingOneOverlappingImportDoesNotRemoveTheOtherDirectory(): Unit = runBlocking {
        withTimeout(15_000.milliseconds) {
            val archive = archive("overlap")
            val locked = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val lock =
                launch(Dispatchers.IO) {
                    db.withTransaction {
                        locked.complete(Unit)
                        release.await()
                    }
                }
            locked.await()
            val first = launch(Dispatchers.IO) { transfer.importCollection(archive.toUri()) }
            var second: Job? = null
            try {
                awaitExtractedDirectories(1)
                val firstDirectory = transferDirectories().single()
                second = launch(Dispatchers.IO) { transfer.importCollection(archive.toUri()) }
                awaitExtractedDirectories(2)
                val secondDirectory = transferDirectories().single { it != firstDirectory }
                first.cancelAndJoin()
                assertFalse(firstDirectory.exists())
                assertTrue(File(secondDirectory, "manifest.json").isFile)
                release.complete(Unit)
                second.join()
                assertEquals(1, collections.observeCollections().first().size)
                assertEquals(1, db.mediaItemDao().getAllIds().size)
                assertNoTransferDirectories()
            } finally {
                release.complete(Unit)
                first.cancelAndJoin()
                second?.cancelAndJoin()
                lock.join()
            }
        }
    }

    private suspend fun awaitExtractedDirectories(count: Int) {
        while (transferDirectories().count { File(it, "media/item.png").isFile } != count) delay(
            10.milliseconds
        )
    }

    private fun transferDirectories() =
        context.cacheDir.listFiles()!!.filter {
            it.name.startsWith("import_") || it.name.startsWith("export_")
        }

    private fun assertNoTransferDirectories() =
        assertTrue(transferDirectories().toString(), transferDirectories().isEmpty())

    private suspend fun assertEmptyDatabaseAndImages() {
        assertTrue(collections.observeCollections().first().isEmpty())
        assertTrue(db.mediaItemDao().getAllIds().isEmpty())
        for (table in listOf("images", "tags", "media_item_tags", "collection_entries")) {
            db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM $table").use {
                assertTrue(it.moveToFirst())
                assertEquals(table, 0, it.getInt(0))
            }
        }
        assertTrue(File(context.filesDir, "images").listFiles().orEmpty().isEmpty())
    }

    private fun archive(name: String): File {
        val file = File(root, "$name.zip")
        val manifest =
            TransferManifest(
                1,
                0,
                TransferCollection(name, "collection/cover.png"),
                listOf(
                    TransferMediaItem("media/item.png", "caption", "description", listOf("tag"))
                ),
            )
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((path, bytes) in
                listOf(
                    "manifest.json" to Json.encodeToString(manifest).toByteArray(),
                    "collection/cover.png" to png(),
                    "media/item.png" to png(),
                )) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return file
    }

    private fun png() = imageBytes(Bitmap.CompressFormat.PNG)

    private fun imageBytes(format: Bitmap.CompressFormat): ByteArray {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        return try {
            bitmap.eraseColor(android.graphics.Color.BLUE)
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(format, 100, output))
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun read(uri: android.net.Uri) =
        context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }

    private suspend inline fun <reified T : Throwable> expectFailure(block: suspend () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            if (error is T) return
            throw error
        }
        fail("Expected ${T::class.java.simpleName}")
    }
}
