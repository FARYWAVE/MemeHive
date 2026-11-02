package com.farywave.memehive.data.local.db

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
import com.farywave.memehive.data.local.db.relation.MediaItemDetails
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RelationReadTest {
    private lateinit var db: MemeHiveDatabase
    private val recordReads = AtomicBoolean(false)
    private val readTransactions = ConcurrentLinkedQueue<Pair<String, Boolean>>()

    @Before
    fun createDatabase() {
        db =
            Room.inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    MemeHiveDatabase::class.java,
                )
                .setQueryCallback(
                    { sqlQuery, _ ->
                        if (
                            recordReads.get() &&
                                sqlQuery.trimStart().startsWith("SELECT", ignoreCase = true)
                        ) {
                            readTransactions.add(sqlQuery to db.inTransaction())
                        }
                    },
                    // Inspect the transaction on the query's own thread, before it executes.
                    { it.run() },
                )
                .build()
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    @Test
    fun everyRelationReaderRunsParentAndChildQueriesInATransaction(): Unit = runBlocking {
        val image = db.imageDao().insert(ImageEntity(name = "A", sha256 = "A"))
        val item =
            db.mediaItemDao()
                .insert(MediaItemEntity(imageId = image, caption = "A", description = ""))
        val tag = db.tagDao().insert(TagEntity(name = "A"))
        db.mediaItemTagDao().insert(MediaItemTagEntity(item, tag))
        val collection =
            db.collectionDao().insert(CollectionEntity(name = "collection", coverImageId = image))
        db.collectionEntryDao().insert(CollectionEntryEntity(collection, item))

        assertTransactionalRead { assertConsistent(db.mediaItemDao().get(item)!!) }
        assertTransactionalRead {
            assertConsistent(db.mediaItemDao().getByIds(listOf(item)).single())
        }
        assertTransactionalRead {
            assertConsistent(db.mediaItemDao().getByCollection(collection).single())
        }
        assertTransactionalRead {
            assertConsistent(db.mediaItemDao().observeAll().first().single())
        }
        assertTransactionalRead {
            assertConsistent(db.mediaItemDao().observeByCollection(collection).first().single())
        }
        assertTransactionalRead {
            assertConsistent(db.mediaItemDao().observeByCollection(null).first().single())
        }
    }

    @Test
    fun concurrentWritesNeverMixParentAndRelatedStates(): Unit = runBlocking {
        val images =
            listOf("A", "B").associateWith { state ->
                db.imageDao().insert(ImageEntity(name = state, sha256 = state))
            }
        val tags = listOf("A", "B").associateWith { db.tagDao().insert(TagEntity(name = it)) }
        val item =
            db.mediaItemDao()
                .insert(
                    MediaItemEntity(imageId = images.getValue("A"), caption = "A", description = "")
                )
        db.mediaItemTagDao().insert(MediaItemTagEntity(item, tags.getValue("A")))
        val updates = Channel<MediaItemDetails>(Channel.UNLIMITED)
        val observer =
            launch(start = CoroutineStart.UNDISPATCHED) {
                db.mediaItemDao().observeAll().collect { rows ->
                    val details = rows.single()
                    assertConsistent(details)
                    updates.send(details)
                }
            }
        try {
            withTimeout(10_000.milliseconds) {
                assertEquals("A", updates.receive().mediaItem.caption)
            }
            withTimeout(20_000.milliseconds) {
                val writer =
                    launch(Dispatchers.IO) {
                        repeat(40) { iteration ->
                            val state = if (iteration % 2 == 0) "A" else "B"
                            db.withTransaction {
                                db.mediaItemDao()
                                    .update(
                                        MediaItemEntity(
                                            item,
                                            images.getValue(state),
                                            state,
                                            iteration.toString(),
                                        )
                                    )
                                yield()
                                db.mediaItemTagDao().deleteByMediaItemId(item)
                                yield()
                                db.mediaItemTagDao()
                                    .insert(MediaItemTagEntity(item, tags.getValue(state)))
                            }
                            yield()
                        }
                    }
                repeat(40) {
                    assertConsistent(db.mediaItemDao().get(item)!!)
                    assertConsistent(db.mediaItemDao().getByIds(listOf(item)).single())
                    yield()
                }
                writer.join()
                var latest = updates.receive()
                while (latest.mediaItem.description != "39") latest = updates.receive()
                assertEquals("B", latest.mediaItem.caption)
                assertConsistent(latest)
            }
        } finally {
            observer.cancel()
            observer.join()
            updates.close()
        }
    }

    private suspend fun assertTransactionalRead(read: suspend () -> Unit) {
        readTransactions.clear()
        recordReads.set(true)
        try {
            read()
        } finally {
            recordReads.set(false)
        }
        assertTrue(
            "Expected parent, tag and image queries: $readTransactions",
            readTransactions.size >= 3,
        )
        assertTrue(
            "A relation query escaped its transaction: $readTransactions",
            readTransactions.all { it.second },
        )
    }

    private fun assertConsistent(details: MediaItemDetails) {
        assertEquals(details.mediaItem.caption, details.tags.single().name)
        assertEquals(details.mediaItem.caption, details.image?.name)
    }
}
