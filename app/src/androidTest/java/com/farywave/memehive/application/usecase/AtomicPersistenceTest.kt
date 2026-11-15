package com.farywave.memehive.application.usecase

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteException
import androidx.core.net.toUri
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.data.local.db.dao.ImageDao
import com.farywave.memehive.data.local.db.entity.ImageEntity
import com.farywave.memehive.data.local.db.entity.MediaItemEntity
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.ImageRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.data.local.db.repository.TagRepository
import com.farywave.memehive.data.local.storage.ImageStorage
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.domain.model.MediaItem
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("SameParameterValue")
@RunWith(AndroidJUnit4::class)
class AtomicPersistenceTest {
    private lateinit var root: File
    private lateinit var context: Context
    private lateinit var db: MemeHiveDatabase
    private lateinit var images: ImageRepository
    private lateinit var collections: CollectionUseCases
    private lateinit var media: MediaItemUseCases

    @Before
    fun setUp() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        root = Files.createTempDirectory(base.cacheDir.toPath(), "atomic_test_").toFile()
        context =
            object : ContextWrapper(base) {
                override fun getApplicationContext(): Context = this

                override fun getFilesDir(): File = File(root, "files").apply { mkdirs() }
            }
        db = Room.inMemoryDatabaseBuilder(base, MemeHiveDatabase::class.java).build()
        images = imageRepository(db.imageDao())
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
    }

    @After
    fun tearDown() {
        db.close()
        root.deleteRecursively()
    }

    @Test
    fun createAndUpdateReplaceAllRelationshipsAndRemoveUnusedImage(): Unit = runBlocking {
        val first = collections.createCollection(Collection(name = "first"))
        val second = collections.createCollection(Collection(name = "second"))
        val original =
            media.createMediaItem(
                MediaItem(
                    image = source("old"),
                    caption = "CAT",
                    description = "DOG",
                    tags = listOf("old"),
                ),
                setOf(first.id),
            )
        assertEquals(setOf(first.id), collections.getCollectionIdsByMediaItem(original.id))
        val oldFile = File(original.image!!.uri.path!!)
        val updated =
            media.updateMediaItem(
                original.copy(
                    image = source("new"),
                    caption = "OWL",
                    description = "FOX",
                    tags = listOf("new"),
                ),
                setOf(second.id),
            )
        assertEquals(updated, media.getMediaItemById(original.id))
        assertEquals(setOf(second.id), collections.getCollectionIdsByMediaItem(original.id))
        assertEquals(listOf("new"), db.tagDao().getAll().map { it.name })
        assertNull(db.imageDao().getById(original.image.id))
        assertFalse(oldFile.exists())
        assertEquals(1, savedFiles().size)
    }

    @Test
    fun failedCreateRollsBackItemImageTagsAndMemberships(): Unit = runBlocking {
        failOn("INSERT", "collection_entries")
        val collection = collections.createCollection(Collection(name = "keep"))
        expectFailure<SQLiteException> {
            media.createMediaItem(
                MediaItem(image = source("create"), caption = "cat", tags = listOf("tag")),
                setOf(collection.id),
            )
        }
        assertEquals(1, count("collections"))
        assertNoItemsOrDependents()
        assertEquals(0, count("images"))
        assertTrue(savedFiles().isEmpty())
    }

    @Test
    fun failedUpdateRestoresOriginalFieldsRelationshipsAndFiles(): Unit = runBlocking {
        val first = collections.createCollection(Collection(name = "first"))
        val second = collections.createCollection(Collection(name = "second"))
        val original =
            media.createMediaItem(
                MediaItem(image = source("old"), caption = "cat", tags = listOf("old")),
                setOf(first.id),
            )
        val filesBefore = savedFiles()
        failOn("INSERT", "collection_entries")
        expectFailure<SQLiteException> {
            media.updateMediaItem(
                original.copy(image = source("replacement"), caption = "dog", tags = listOf("new")),
                setOf(second.id),
            )
        }
        assertEquals(original, media.getMediaItemById(original.id))
        assertEquals(setOf(first.id), collections.getCollectionIdsByMediaItem(original.id))
        assertEquals(listOf("old"), db.tagDao().getAll().map { it.name })
        assertEquals(filesBefore, savedFiles())
        assertEquals(1, count("images"))
    }

    @Test
    fun missingUpdateTargetsRollBackNewImages(): Unit = runBlocking {
        expectFailure<IllegalStateException> {
            media.updateMediaItem(MediaItem(id = 999, image = source("missing-item")))
        }
        expectFailure<IllegalStateException> {
            collections.updateCollection(
                Collection(id = 999, name = "missing", cover = source("missing-cover"))
            )
        }
        assertNoItemsOrDependents()
        assertEquals(0, count("collections"))
        assertEquals(0, count("images"))
        assertTrue(savedFiles().isEmpty())
    }

    @Test
    fun removingImageAndRelationshipsClearsOnlyUnusedData(): Unit = runBlocking {
        val collection = collections.createCollection(Collection(name = "collection"))
        val item =
            media.createMediaItem(
                MediaItem(image = source("remove"), caption = "cat", tags = listOf("tag")),
                setOf(collection.id),
            )
        val updated =
            media.updateMediaItem(
                item.copy(image = null, caption = "", tags = emptyList()),
                emptySet(),
            )
        assertEquals(updated, media.getMediaItemById(item.id))
        assertTrue(collections.getCollectionIdsByMediaItem(item.id).isEmpty())
        assertEquals(0, count("tags"))
        assertEquals(0, count("images"))
        assertTrue(savedFiles().isEmpty())
    }

    @Test
    fun sharedImageSurvivesItemDeletionUntilCollectionCoverIsDeleted(): Unit = runBlocking {
        val source = source("shared")
        val first =
            media.createMediaItem(
                MediaItem(image = source, caption = "cat", tags = listOf("shared"))
            )
        val second =
            media.createMediaItem(
                MediaItem(image = source, caption = "dog", tags = listOf("shared"))
            )
        val collection = collections.createCollection(Collection(name = "cover", cover = source))
        val imageId = first.image!!.id
        val file = File(first.image.uri.path!!)
        assertEquals(imageId, second.image!!.id)
        assertEquals(imageId, collection.cover!!.id)
        assertEquals(1, savedFiles().size)
        media.deleteMediaItem(first)
        assertEquals(listOf("shared"), db.tagDao().getAll().map { it.name })
        media.deleteMediaItem(second)
        assertTrue(file.exists())
        assertNotNull(db.imageDao().getById(imageId))
        assertEquals(0, count("tags"))
        collections.deleteCollection(collection)
        assertFalse(file.exists())
        assertNull(db.imageDao().getById(imageId))
        assertNoItemsOrDependents()
    }

    @Test
    fun coverReplacementAndMissingCoverTargetPreserveSharedImages(): Unit = runBlocking {
        val item = media.createMediaItem(MediaItem(image = source("shared-cover")))
        val collection =
            collections.createCollection(Collection(name = "cover", cover = item.image))
        val updated = collections.updateCollection(collection.copy(cover = source("new-cover")))
        assertNotNull(db.imageDao().getById(item.image!!.id))
        assertEquals(2, savedFiles().size)
        val replacedFile = File(updated.cover!!.uri.path!!)
        collections.setCollectionCover(collection.id, item.image.id)
        assertFalse(replacedFile.exists())
        assertEquals(item.image.id, db.collectionDao().getCoverId(collection.id))
        expectFailure<IllegalStateException> { collections.setCollectionCover(999, item.image.id) }
        assertEquals(1, savedFiles().size)
        assertNotNull(db.imageDao().getById(item.image.id))
    }

    @Test
    fun bulkDeleteHandlesMultipleBatchesNullImagesAndSharedReferences(): Unit = runBlocking {
        val collection =
            collections.createCollection(
                Collection(name = "keep-cover", cover = source("bulk-shared"))
            )
        val item =
            media.createMediaItem(
                MediaItem(image = source("bulk-only"), caption = "cat", tags = listOf("delete")),
                setOf(collection.id),
            )
        val outside =
            media.createMediaItem(MediaItem(image = collection.cover, tags = listOf("keep")))
        val ids = bulkItems(collection.cover!!.id).apply { add(item.id) }
        collections.addItemsToCollection(ids, collection.id)
        media.deleteMediaItemsByIds(ids)
        assertEquals(listOf(outside.id), db.mediaItemDao().getAllIds())
        assertEquals(0, count("collection_entries"))
        assertEquals(listOf("keep"), db.tagDao().getAll().map { it.name })
        assertEquals(1, count("images"))
        assertEquals(1, savedFiles().size)
        assertNotNull(db.imageDao().getById(collection.cover.id))
        media.deleteMediaItemsByIds(ids)
        media.deleteMediaItemsByIds(emptySet())
        assertEquals(listOf(outside.id), db.mediaItemDao().getAllIds())
    }

    @Test
    fun failureInLaterBulkBatchRollsBackEarlierDeletions(): Unit = runBlocking {
        val item =
            media.createMediaItem(
                MediaItem(image = source("bulk-rollback"), caption = "cat", tags = listOf("keep"))
            )
        val ids = linkedSetOf(item.id).apply { addAll(bulkItems(null)) }
        val filesBefore = savedFiles()
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_late_delete BEFORE DELETE ON media_items
            WHEN OLD.id = ${ids.last()}
            BEGIN SELECT RAISE(ABORT, 'forced later-batch failure'); END
        """
                .trimIndent()
        )
        expectFailure<SQLiteException> { media.deleteMediaItemsByIds(ids) }
        assertEquals(ids, db.mediaItemDao().getAllIds().toSet())
        assertEquals(item, media.getMediaItemById(item.id))
        assertEquals(filesBefore, savedFiles())
        assertEquals(1, count("images"))
    }

    @Test
    fun deletingCollectionByDefaultPreservesItsContents(): Unit = runBlocking {
        val collection = collections.createCollection(Collection(name = "delete"))
        val item =
            media.createMediaItem(
                MediaItem(image = source("keep"), tags = listOf("keep")),
                setOf(collection.id),
            )
        collections.deleteCollection(collection)
        assertEquals(0, count("collections"))
        assertEquals(0, count("collection_entries"))
        assertEquals(item, media.getMediaItemById(item.id))
        assertTrue(File(item.image!!.uri.path!!).exists())
        assertEquals(listOf("keep"), db.tagDao().getAll().map { it.name })
    }

    @Test
    fun deletingCollectionContentsPreservesItemsInOtherCollections(): Unit = runBlocking {
        val collection =
            collections.createCollection(Collection(name = "delete", cover = source("cover")))
        val other = collections.createCollection(Collection(name = "keep"))
        val item =
            media.createMediaItem(
                MediaItem(image = source("item"), tags = listOf("shared")),
                setOf(collection.id, other.id),
            )
        val exclusive =
            media.createMediaItem(
                MediaItem(image = collection.cover, tags = listOf("delete")),
                setOf(collection.id),
            )
        val outside =
            media.createMediaItem(
                MediaItem(image = item.image, tags = listOf("keep")),
                setOf(other.id),
            )
        val coverFile = File(collection.cover!!.uri.path!!)
        collections.deleteCollection(collection, deleteContents = true)
        assertEquals(setOf(item.id, outside.id), db.mediaItemDao().getAllIds().toSet())
        assertEquals(item, media.getMediaItemById(item.id))
        assertEquals(setOf(other.id), collections.getCollectionIdsByMediaItem(item.id))
        assertNull(media.getMediaItemById(exclusive.id))
        assertEquals(1, count("collections"))
        assertEquals(2, count("collection_entries"))
        assertEquals(setOf("keep", "shared"), db.tagDao().getAll().map { it.name }.toSet())
        assertEquals(1, count("images"))
        assertFalse(coverFile.exists())
        assertTrue(File(outside.image!!.uri.path!!).exists())
        collections.deleteCollection(other, deleteContents = true)
        assertNoItemsOrDependents()
        assertTrue(savedFiles().isEmpty())
        val empty = collections.createCollection(Collection(name = "empty"))
        collections.deleteCollection(empty, deleteContents = true)
        assertEquals(0, count("collections"))
    }

    @Test
    fun failedCollectionDeletionRestoresContentsAndTheirFiles(): Unit = runBlocking {
        val collection =
            collections.createCollection(Collection(name = "restore", cover = source("cover")))
        val item =
            media.createMediaItem(
                MediaItem(image = source("item"), tags = listOf("restore")),
                setOf(collection.id),
            )
        val filesBefore = savedFiles()
        failOn("DELETE", "collections")
        expectFailure<SQLiteException> {
            collections.deleteCollection(collection, deleteContents = true)
        }
        assertEquals(1, count("collections"))
        assertEquals(item, media.getMediaItemById(item.id))
        assertEquals(setOf(collection.id), collections.getCollectionIdsByMediaItem(item.id))
        assertEquals(listOf("restore"), db.tagDao().getAll().map { it.name })
        assertEquals(2, count("images"))
        assertEquals(filesBefore, savedFiles())
    }

    @Test
    fun savedHashMatchesCopiedBytesWhenOriginalSourceChanges(): Unit = runBlocking {
        val sourceFile = File(root, "changing.png").apply { writeText("original bytes") }
        val originalBytes = sourceFile.readBytes()
        val dao =
            object : ImageDao by db.imageDao() {
                override suspend fun getBySha256(sha256: String): ImageEntity? {
                    sourceFile.writeText("changed bytes")
                    return db.imageDao().getBySha256(sha256)
                }
            }
        val repository = imageRepository(dao)
        val image = repository.withImageChanges { changes ->
            db.withTransaction { repository.insertOrGet(sourceFile.toUri(), changes) }
        }
        val saved = File(image.uri.path!!).readBytes()
        assertArrayEquals(originalBytes, saved)
        assertEquals("changed bytes", sourceFile.readText())
        assertEquals(hash(saved), db.imageDao().getById(image.id)!!.sha256)
    }

    @Test
    fun concurrentIdenticalCreatesReuseImageAndTagAndDiscardExtraCopies(): Unit = runBlocking {
        val image = source("concurrent")
        val items = coroutineScope {
            List(4) {
                    async(Dispatchers.IO) {
                        media.createMediaItem(
                            MediaItem(image = image, caption = "cat", tags = listOf("shared"))
                        )
                    }
                }
                .awaitAll()
        }
        assertEquals(4, items.map { it.id }.toSet().size)
        assertEquals(1, items.map { it.image!!.id }.toSet().size)
        assertEquals(1, count("images"))
        assertEquals(1, count("tags"))
        assertEquals(4, count("media_item_tags"))
        assertEquals(1, savedFiles().size)
    }

    private fun imageRepository(dao: ImageDao): ImageRepository =
        ImageRepository(context, dao, Dispatchers.IO, ImageStorage(context, Dispatchers.IO))

    private fun source(contents: String): Image {
        val file = File.createTempFile("source_", ".png", root).apply { writeText(contents) }
        return Image(uri = file.toUri())
    }

    private suspend fun bulkItems(sharedImageId: Long?): MutableSet<Long> = db.withTransaction {
        (0..3000).mapTo(linkedSetOf()) { index ->
            db.mediaItemDao()
                .insert(
                    MediaItemEntity(
                        imageId = if (index % 2 == 0) sharedImageId else null,
                        caption = "bulk",
                        description = "",
                    )
                )
        }
    }

    private fun savedFiles(): Set<String> =
        File(context.filesDir, "images").listFiles().orEmpty().map { it.name }.toSet()

    private fun count(table: String): Int =
        db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM $table").use {
            check(it.moveToFirst())
            it.getInt(0)
        }

    private fun assertNoItemsOrDependents() {
        for (table in listOf("media_items", "tags", "media_item_tags", "collection_entries")) {
            assertEquals(table, 0, count(table))
        }
    }

    private fun failOn(operation: String, table: String) {
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER forced_failure BEFORE $operation ON $table
            BEGIN SELECT RAISE(ABORT, 'forced test failure'); END
        """
                .trimIndent()
        )
    }

    private fun hash(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

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
