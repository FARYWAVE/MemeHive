package com.farywave.memehive.data.local.db

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import com.farywave.memehive.application.usecase.MediaSearchUseCase
import com.farywave.memehive.data.local.db.entity.*
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.domain.model.MediaItem
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class ReactiveSearchTest {
    @Test
    fun roomInvalidationUpdatesSearchAfterImportEditTagAndMembershipChanges() = runBlocking {
        val db =
            Room.inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    MemeHiveDatabase::class.java,
                )
                .build()
        val updates = Channel<List<MediaItem>>(Channel.UNLIMITED)
        val collectionId =
            db.collectionDao().insert(CollectionEntity(name = "Collection", coverImageId = null))
        val search =
            MediaSearchUseCase(
                MediaItemRepository(db.mediaItemDao()) { error("No images") },
                Dispatchers.Default,
            )
        val job = launch {
            search(MutableStateFlow<Long?>(collectionId), MutableStateFlow("cat")).collect {
                updates.send(it)
            }
        }
        suspend fun awaitIds(vararg ids: Long) =
            withTimeout(5_000.milliseconds) {
                while (true) {
                    val result = updates.receive()
                    if (result.map { it.id } == ids.toList()) break
                }
            }
        try {
            awaitIds()
            // A transactional import becomes visible through the existing subscription.
            val id = db.withTransaction {
                val id =
                    db.mediaItemDao()
                        .insert(MediaItemEntity(caption = "cat", description = "", imageId = null))
                db.collectionEntryDao().insert(CollectionEntryEntity(collectionId, id))
                id
            }
            awaitIds(id)
            db.mediaItemDao()
                .update(MediaItemEntity(id, imageId = null, caption = "dog", description = ""))
            awaitIds()
            val tag = db.tagDao().insert(TagEntity(name = "cat"))
            db.mediaItemTagDao().insert(MediaItemTagEntity(id, tag))
            awaitIds(id)
            db.tagDao().delete(tag)
            awaitIds()
            db.mediaItemDao()
                .update(MediaItemEntity(id, imageId = null, caption = "cat", description = ""))
            awaitIds(id)
            db.collectionEntryDao().delete(collectionId, id)
            awaitIds()
            db.collectionEntryDao().insert(CollectionEntryEntity(collectionId, id))
            awaitIds(id)
            db.mediaItemDao().delete(id)
            awaitIds()
            assertEquals(emptyList<Long>(), db.mediaItemDao().getAllIds())
        } finally {
            job.cancelAndJoin()
            updates.close()
            db.close()
        }
    }
}
