package com.farywave.memehive.ui.use_case.media_item_search

import com.farywave.memehive.data.local.db.dao.MediaItemDao
import com.farywave.memehive.data.local.db.entity.MediaItemEntity
import com.farywave.memehive.data.local.db.entity.TagEntity
import com.farywave.memehive.data.local.db.relation.MediaItemDetails
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.domain.model.MediaItem
import com.farywave.memehive.ui.use_case.MediaSearchUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaSearchUseCaseTest {
    @Test
    fun `inserts edits tag changes and deletes update results without refreshing`() = runTest {
        val search = observeSearch()
        search.query.value = "fu"
        runCurrent()
        assertTrue(search.latest.isEmpty())

        search.dao.items.value = listOf(item(1, caption = "Funny caption"))
        runCurrent()
        assertEquals(listOf(1L), search.ids)

        search.dao.items.value = listOf(item(1, caption = "Other caption"))
        runCurrent()
        assertTrue(search.latest.isEmpty())

        search.dao.items.value = listOf(item(1, tags = listOf("funny")))
        runCurrent()
        assertEquals(listOf("funny"), search.latest.single().tags)

        search.dao.items.value = listOf(item(1, tags = listOf("other")))
        runCurrent()
        assertTrue(search.latest.isEmpty())

        search.dao.items.value = listOf(item(1, description = "Fun description"))
        runCurrent()
        assertEquals(listOf(1L), search.ids)

        search.dao.items.value = emptyList()
        runCurrent()
        assertTrue(search.latest.isEmpty())
    }

    @Test
    fun `null observes all and collection membership changes update the scope`() = runTest {
        val search = observeSearch()
        search.dao.items.value = listOf(item(1), item(2))
        search.dao.memberships.value = mapOf(7L to setOf(1L), 8L to setOf(2L))
        runCurrent()
        assertEquals(listOf(1L, 2L), search.ids)

        search.collectionId.value = 7L
        runCurrent()
        assertEquals(listOf(1L), search.ids)
        assertEquals(setOf(7L), search.dao.activeScopes)

        search.dao.memberships.value = mapOf(7L to setOf(2L), 8L to setOf(1L))
        runCurrent()
        assertEquals(listOf(2L), search.ids)

        search.collectionId.value = 8L
        runCurrent()
        assertEquals(listOf(1L), search.ids)
        assertEquals(setOf(8L), search.dao.activeScopes)

        // Updating the old scope must not replace the current collection's results.
        search.dao.memberships.value = mapOf(7L to emptySet(), 8L to setOf(1L))
        runCurrent()
        assertEquals(listOf(1L), search.ids)

        search.collectionId.value = null
        runCurrent()
        assertEquals(listOf(1L, 2L), search.ids)
        assertEquals(setOf<Long?>(null), search.dao.activeScopes)
    }

    @Test
    fun `query changes use latest input without restarting database observation`() = runTest {
        val search = observeSearch()
        search.dao.items.value = listOf(item(1, caption = "funny"), item(2, caption = "cat"))
        runCurrent()

        search.query.value = "fu"
        runCurrent()
        assertEquals(listOf(1L), search.ids)

        search.query.value = "fun"
        search.query.value = "missing"
        search.query.value = "cat"
        runCurrent()
        assertEquals(listOf(2L), search.ids)

        search.dao.items.value += item(3, caption = "funny too")
        runCurrent()
        assertEquals(listOf(2L), search.ids)
        assertEquals(1, search.dao.observationCount)
    }

    @Test
    fun `tag prefixes rank before tag substrings captions and descriptions`() = runTest {
        val search = observeSearch()
        search.dao.items.value = listOf(
            item(1, description = "Fun description"),
            item(2, caption = "Fun caption"),
            item(3, tags = listOf("kung fu")),
            item(4, tags = listOf("Funny")),
            item(5, tags = listOf("unrelated")),
            item(6, caption = "fun", tags = listOf("fun")),
        )
        search.query.value = " FU "
        runCurrent()
        assertEquals(listOf(4L, 6L, 3L, 2L, 1L), search.ids)

        search.query.value = " \n "
        runCurrent()
        assertEquals((1L..6L).toList(), search.ids)
    }

    @Test
    fun `extending queries never introduces results absent for shorter prefixes`() = runTest {
        val search = observeSearch()
        val texts = listOf(
            "Funny", "fundamental", "kung fu", "two words", "100%_literal",
            "Привет", "ΟΣΑ", "İstanbul",
        )
        search.dao.items.value = texts.flatMapIndexed { index, text ->
            listOf(
                item(index * 3L + 1, tags = listOf(text)),
                item(index * 3L + 2, caption = text),
                item(index * 3L + 3, description = text),
            )
        }
        for (query in texts + listOf("funnier", "funz", "missing")) {
            search.query.value = ""
            runCurrent()
            var previous = search.ids.toSet()
            for (length in 1..query.length) {
                val prefix = query.take(length)
                search.query.value = prefix
                runCurrent()
                val current = search.ids.toSet()
                assertTrue("New results appeared for '$prefix'", previous.containsAll(current))
                previous = current
            }
            if (query in texts) assertEquals(3, search.ids.size)
        }
    }

    @Test
    fun `one and two characters search captions and descriptions too`() = runTest {
        val search = observeSearch()
        search.dao.items.value = listOf(
            item(1, caption = "Funny"),
            item(2, description = "Fun afternoon"),
            item(3, tags = listOf("fun")),
        )
        for (query in listOf("f", "fu", "fun")) {
            search.query.value = query
            runCurrent()
            assertEquals(setOf(1L, 2L, 3L), search.ids.toSet())
        }
        // Sharing a trigram alone is no longer enough to match an unrelated query.
        search.query.value = "funz"
        runCurrent()
        assertTrue(search.latest.isEmpty())
    }

    private fun TestScope.observeSearch(): SearchFixture {
        val fixture = SearchFixture()
        val search = MediaSearchUseCase(
            MediaItemRepository(fixture.dao),
            StandardTestDispatcher(testScheduler),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            search(fixture.collectionId, fixture.query).collect { fixture.latest = it }
        }
        return fixture
    }

    private class SearchFixture {
        val dao = FakeMediaItemDao()
        val collectionId = MutableStateFlow<Long?>(null)
        val query = MutableStateFlow("")
        var latest: List<MediaItem> = emptyList()
        val ids get() = latest.map { it.id }
    }

    private fun item(
        id: Long,
        caption: String = "",
        description: String = "",
        tags: List<String> = emptyList(),
    ) = MediaItemDetails(
        mediaItem = MediaItemEntity(id, imageId = null, caption, description),
        tags = tags.mapIndexed { index, name -> TagEntity(index.toLong(), name) },
    )

    private class FakeMediaItemDao : MediaItemDao {
        val items = MutableStateFlow<List<MediaItemDetails>>(emptyList())
        val memberships = MutableStateFlow<Map<Long, Set<Long>>>(emptyMap())
        val activeScopes = mutableSetOf<Long?>()
        var observationCount = 0

        override fun observeByCollection(collectionId: Long?): Flow<List<MediaItemDetails>> = flow {
            observationCount++
            activeScopes += collectionId
            try {
                emitAll(combine(items, memberships) { items, memberships ->
                    items.filter {
                        collectionId == null || it.mediaItem.id in memberships[collectionId].orEmpty()
                    }
                })
            } finally {
                activeScopes -= collectionId
            }
        }

        override suspend fun insert(mediaItem: MediaItemEntity): Long = error("Unused")
        override suspend fun update(mediaItem: MediaItemEntity): Unit = error("Unused")
        override suspend fun delete(mediaItemId: Long): Unit = error("Unused")
        override suspend fun deleteByIds(mediaItemIds: Set<Long>): Unit = error("Unused")
        override fun getAll(): Flow<List<MediaItemDetails>> = error("Unused")
        override suspend fun get(mediaItemId: Long): MediaItemDetails? = error("Unused")
        override suspend fun getIdsByTags(tagIds: List<Long>, tagCount: Int): List<Long> = error("Unused")
        override suspend fun getByIds(mediaItemIds: List<Long>): List<MediaItemDetails> = error("Unused")
        override suspend fun getByCollection(collectionId: Long): List<MediaItemDetails> = error("Unused")
        override suspend fun getAllIds(): List<Long> = error("Unused")
        override suspend fun getImageId(mediaItemId: Long): Long? = error("Unused")
        override suspend fun getImageIds(mediaItemIds: Set<Long>): List<Long> = error("Unused")
    }
}
