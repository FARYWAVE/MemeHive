package com.farywave.memehive.data.local.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.farywave.memehive.data.local.db.entity.CollectionEntity
import com.farywave.memehive.data.local.db.entity.CollectionEntryEntity
import com.farywave.memehive.data.local.db.entity.ImageEntity
import com.farywave.memehive.data.local.db.entity.MediaItemEntity
import com.farywave.memehive.data.local.db.entity.MediaItemTagEntity
import com.farywave.memehive.data.local.db.entity.TagEntity
import com.farywave.memehive.data.local.db.relation.CollectionDetails
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseBehaviorTest {
    private lateinit var db: MemeHiveDatabase

    @Before
    fun createDatabase() {
        db =
            Room.inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    MemeHiveDatabase::class.java,
                )
                .build()
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    @Test
    fun imageCleanupIdsExcludeItemsWithoutImages(): Unit = runBlocking {
        val image = db.imageDao().insert(ImageEntity(name = "cleanup.png", sha256 = "cleanup"))
        val withImage = insertItem(image)
        val withoutImage = insertItem()
        assertEquals(listOf(image), db.mediaItemDao().getImageIds(setOf(withImage, withoutImage)))
        assertTrue(db.mediaItemDao().getImageIds(setOf(withoutImage)).isEmpty())
        assertTrue(db.mediaItemDao().getImageIds(emptySet()).isEmpty())
    }

    @Test
    fun freshVersion5RejectsMissingParents(): Unit = runBlocking {
        assertEquals(5, db.openHelper.writableDatabase.version)
        val item = insertItem()
        val collection = insertCollection()
        val tag = db.tagDao().insert(TagEntity(name = "tag"))
        assertConstraint {
            db.mediaItemDao()
                .insert(MediaItemEntity(imageId = 999, caption = "bad", description = ""))
        }
        assertConstraint {
            db.collectionDao().insert(CollectionEntity(name = "bad", coverImageId = 999))
        }
        assertConstraint { db.collectionDao().setCover(collection, 999) }
        assertConstraint { db.collectionEntryDao().insert(CollectionEntryEntity(collection, 999)) }
        assertConstraint { db.collectionEntryDao().insert(CollectionEntryEntity(999, item)) }
        assertConstraint { db.mediaItemTagDao().insert(MediaItemTagEntity(999, tag)) }
        assertConstraint { db.mediaItemTagDao().insert(MediaItemTagEntity(item, 999)) }
        assertEquals(listOf(item), db.mediaItemDao().getAllIds())
        db.openHelper.writableDatabase.assertForeignKeysValid()
    }

    @Test
    fun deletingSharedImageClearsReferencesAndPreservesOwners(): Unit = runBlocking {
        val image = db.imageDao().insert(ImageEntity(name = "/image.png", sha256 = "hash"))
        val item = insertItem(image)
        val collection = insertCollection(image)
        db.collectionEntryDao().insert(CollectionEntryEntity(collection, item))
        db.imageDao().delete(image)
        val details = db.mediaItemDao().get(item)!!
        assertNull(details.mediaItem.imageId)
        assertNull(details.image)
        val summary = db.collectionDao().observeAll().first().single()
        assertEquals(collection, summary.collection.id)
        assertNull(summary.collection.coverImageId)
        assertNull(summary.coverImage)
        assertEquals(1, summary.mediaItemCount)
        db.openHelper.writableDatabase.assertForeignKeysValid()
    }

    @Test
    fun deletingItemCascadesToItsDependentsOnly(): Unit = runBlocking {
        val item = insertItem()
        val otherItem = insertItem()
        val collection = insertCollection()
        val tag = db.tagDao().insert(TagEntity(name = "shared"))
        for (id in listOf(item, otherItem)) {
            db.collectionEntryDao().insert(CollectionEntryEntity(collection, id))
            db.mediaItemTagDao().insert(MediaItemTagEntity(id, tag))
        }
        db.mediaItemDao().delete(item)
        assertEquals(listOf(otherItem), db.collectionEntryDao().getIdsByCollection(collection))
        assertEquals(
            listOf(otherItem),
            db.mediaItemTagDao().observeByTag(tag).first().map { it.mediaItemId },
        )
        assertNotNull(db.tagDao().getByName("shared"))
        assertEquals(collection, db.collectionDao().observeAll().first().single().collection.id)
        db.openHelper.writableDatabase.assertForeignKeysValid()
    }

    @Test
    fun deletingCollectionAndTagPreservesMediaItems(): Unit = runBlocking {
        val item = insertItem()
        val collection = insertCollection()
        val tag = db.tagDao().insert(TagEntity(name = "tag"))
        db.collectionEntryDao().insert(CollectionEntryEntity(collection, item))
        db.mediaItemTagDao().insert(MediaItemTagEntity(item, tag))
        db.collectionDao().delete(collection)
        db.tagDao().delete(tag)
        assertTrue(db.collectionEntryDao().getByMediaItem(item).isEmpty())
        assertTrue(db.mediaItemTagDao().observeByTag(tag).first().isEmpty())
        assertNotNull(db.mediaItemDao().get(item))
        db.openHelper.writableDatabase.assertForeignKeysValid()
    }

    @Test
    fun existingSummarySubscriptionTracksMembershipsAndCovers(): Unit = runBlocking {
        val updates = Channel<List<CollectionDetails>>(Channel.UNLIMITED)
        val collector =
            launch(start = CoroutineStart.UNDISPATCHED) {
                db.collectionDao().observeAll().collect { updates.send(it) }
            }
        try {
            updates.await { it.isEmpty() }
            val collection = insertCollection()
            updates.await { it.size == 1 && it.single().mediaItemCount == 0 }
            val first = insertItem()
            val second = insertItem()
            db.collectionEntryDao()
                .insert(
                    listOf(
                        CollectionEntryEntity(collection, first),
                        CollectionEntryEntity(collection, second),
                    )
                )
            updates.await { it.single().mediaItemCount == 2 }
            db.collectionEntryDao().delete(collection, first)
            updates.await { it.single().mediaItemCount == 1 }
            val image = db.imageDao().insert(ImageEntity(name = "/cover.png", sha256 = "cover"))
            db.collectionDao().setCover(collection, image)
            updates.await { it.single().coverImage?.id == image }
            val emptyCollection = insertCollection(image)
            updates.await { rows ->
                rows.size == 2 &&
                    rows.all { it.coverImage?.id == image } &&
                    rows.associate { it.collection.id to it.mediaItemCount } ==
                        mapOf(collection to 1, emptyCollection to 0)
            }
            db.imageDao().delete(image)
            updates.await { rows -> rows.size == 2 && rows.all { it.coverImage == null } }
            db.mediaItemDao().delete(second)
            updates.await { rows -> rows.size == 2 && rows.all { it.mediaItemCount == 0 } }
            db.collectionDao().delete(collection)
            updates.await { it.size == 1 && it.single().collection.id == emptyCollection }
        } finally {
            collector.cancel()
            collector.join()
            updates.close()
        }
    }

    @Test
    fun transactionalMoveProducesConsistentSummaryCounts(): Unit = runBlocking {
        val item = insertItem()
        val first = insertCollection()
        val second = insertCollection()
        db.collectionEntryDao().insert(CollectionEntryEntity(first, item))
        val updates = Channel<List<CollectionDetails>>(Channel.UNLIMITED)
        val collector =
            launch(start = CoroutineStart.UNDISPATCHED) {
                db.collectionDao().observeAll().collect { rows ->
                    assertEquals(1, rows.sumOf { it.mediaItemCount })
                    updates.send(rows)
                }
            }
        try {
            updates.await { it.first().mediaItemCount == 1 }
            db.withTransaction {
                db.collectionEntryDao().delete(first, item)
                yield()
                db.collectionEntryDao().insert(CollectionEntryEntity(second, item))
            }
            updates.await { it.last().mediaItemCount == 1 }
        } finally {
            collector.cancel()
            collector.join()
            updates.close()
        }
    }

    @Test
    fun representativeLookupsUseTheNewIndexes() {
        val queries =
            mapOf(
                "SELECT id FROM media_items WHERE image_id = 1" to "index_media_items_image_id",
                "SELECT id FROM collections WHERE cover_image_id = 1" to
                    "index_collections_cover_image_id",
                "SELECT collection_id FROM collection_entries WHERE media_item_id = 1" to
                    "index_collection_entries_media_item_id",
                "SELECT media_item_id FROM media_item_tags WHERE tag_id = 1" to
                    "index_media_item_tags_tag_id",
            )
        for ((query, index) in queries) {
            val plan =
                db.openHelper.writableDatabase
                    .rows("EXPLAIN QUERY PLAN $query")
                    .flatten()
                    .joinToString(" ")
            assertTrue(
                "Expected lookup through $index, got $plan",
                plan.contains(index) && plan.contains("SEARCH"),
            )
        }
    }

    private suspend fun insertItem(image: Long? = null): Long =
        db.mediaItemDao()
            .insert(MediaItemEntity(imageId = image, caption = "item", description = ""))

    private suspend fun insertCollection(image: Long? = null): Long =
        db.collectionDao().insert(CollectionEntity(name = "collection", coverImageId = image))

    private suspend fun assertConstraint(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected SQLite to reject an invalid relationship or duplicate")
        } catch (_: SQLiteConstraintException) {
        }
    }

    private suspend fun Channel<List<CollectionDetails>>.await(
        predicate: (List<CollectionDetails>) -> Boolean
    ): List<CollectionDetails> =
        withTimeout(10_000.milliseconds) {
            var rows = receive()
            while (!predicate(rows)) rows = receive()
            rows
        }
}
