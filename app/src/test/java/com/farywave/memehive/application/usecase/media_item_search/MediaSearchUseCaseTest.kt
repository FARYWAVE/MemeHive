package com.farywave.memehive.application.usecase.media_item_search

import com.farywave.memehive.application.usecase.MediaSearchState
import com.farywave.memehive.application.usecase.MediaSearchUseCase
import com.farywave.memehive.data.local.db.dao.MediaItemDao
import com.farywave.memehive.data.local.db.entity.MediaItemEntity
import com.farywave.memehive.data.local.db.entity.TagEntity
import com.farywave.memehive.data.local.db.relation.MediaItemDetails
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.domain.model.MediaItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
        search.dao.items.value =
            listOf(
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
    fun `normalization handles compatibility characters composed accents and whitespace`() =
        runTest {
            val search = observeSearch()
            search.dao.items.value =
                listOf(
                    item(1, caption = "ＣＡＦÉ  cats"),
                    item(2, description = "Привет"),
                    item(3, caption = "100%_literal"),
                )
            for ((query, ids) in
                listOf(
                    " cafe\u0301 \t cats " to listOf(1L),
                    "ПРИ" to listOf(2L),
                    "%_" to listOf(3L),
                    "  " to listOf(1L, 2L, 3L),
                )) {
                search.query.value = query
                runCurrent()
                assertEquals(ids, search.ids)
            }
        }

    @Test
    fun `one edit typos include transpositions but short queries remain literal`() = runTest {
        val search = observeSearch()
        search.dao.items.value = listOf(item(1, caption = "funny"), item(2, caption = "cat"))
        for (query in listOf("funy", "funnny", "fanny", "funyn")) {
            search.query.value = query
            runCurrent()
            assertEquals(query, listOf(1L), search.ids)
        }
        for (query in listOf("fxnxy", "cta", "funny dog")) {
            search.query.value = query
            runCurrent()
            assertTrue(query, search.ids.isEmpty())
        }
    }

    @Test
    fun `exact matches precede typos with field similarity and id tie breaks`() = runTest {
        val search = observeSearch()
        search.dao.items.value =
            listOf(
                item(9, tags = listOf("funnys")),
                item(8, tags = listOf("funny")),
                item(7, tags = listOf("very funny")),
                item(6, caption = "funny"),
                item(5, description = "funny"),
                item(4, tags = listOf("fanny")),
                item(3, tags = listOf("funnny")),
                item(2, caption = "fanny"),
                item(1, description = "fanny"),
                item(10, tags = listOf("funny")),
            )
        search.query.value = "funny"
        runCurrent()
        assertEquals(listOf(8L, 10L, 9L, 7L, 6L, 5L, 3L, 4L, 2L, 1L), search.ids)
    }

    @Test
    fun `loading failure and retry recover observation and subsequent edits`() = runTest {
        val dao = FakeMediaItemDao().apply { failuresRemaining = 1 }
        val retries = MutableStateFlow(0)
        val states = mutableListOf<MediaSearchState>()
        val useCase =
            MediaSearchUseCase(
                MediaItemRepository(dao) { error("No images") },
                StandardTestDispatcher(testScheduler),
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase.observeState(MutableStateFlow(null), MutableStateFlow(""), retries).collect {
                states += it
            }
        }
        runCurrent()
        assertTrue(states.first().isLoading)
        assertTrue(states.last().failure is IllegalStateException)
        retries.value++
        runCurrent()
        assertEquals(MediaSearchState(), states.last())
        dao.items.value = listOf(item(1, caption = "new"))
        runCurrent()
        assertEquals(listOf(1L), states.last().items.map { it.id })
        assertEquals(2, dao.observationCount)
    }

    @Test
    fun `switching collection cancels a delayed read and clears stale results while loading`() =
        runTest {
            val dao =
                FakeMediaItemDao().apply {
                    items.value = listOf(item(1), item(2))
                    memberships.value = mapOf(7L to setOf(1L), 8L to setOf(2L))
                    delays = mapOf(7L to 1000L)
                }
            val collection = MutableStateFlow<Long?>(null)
            val states = mutableListOf<MediaSearchState>()
            val useCase =
                MediaSearchUseCase(
                    MediaItemRepository(dao) { error("No images") },
                    StandardTestDispatcher(testScheduler),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                useCase
                    .observeState(collection, MutableStateFlow(""), MutableStateFlow(0))
                    .collect { states += it }
            }
            runCurrent()
            collection.value = 7L
            runCurrent()
            assertTrue(states.last().isLoading)
            assertTrue(states.last().items.isEmpty())
            collection.value = 8L
            runCurrent()
            val switchIndex = states.size - 1
            advanceTimeBy(2000)
            runCurrent()
            assertTrue(
                states.drop(switchIndex).all { it.items.map { item -> item.id } == listOf(2L) }
            )
            assertEquals(setOf<Long?>(8L), dao.activeScopes)
        }

    @Test
    fun `ten thousand items support repeated queries without reloading the database`() = runTest {
        val search = observeSearch()
        search.dao.items.value =
            (1L..10_000L).map {
                item(
                    it,
                    caption = "Library photo number $it",
                    description = "A sample description for a realistic library",
                    tags = listOf("photos", "archive"),
                )
            }
        val started = System.nanoTime()
        runCurrent()
        for (query in listOf("photo", "phoot", "archive", "missing", "number 9999")) {
            search.query.value = query
            runCurrent()
            when (query) {
                "missing" -> assertTrue(search.ids.isEmpty())
                "number 9999" -> assertEquals(listOf(9999L), search.ids)
                else -> assertEquals(10_000, search.ids.size)
            }
        }
        assertEquals(1, search.dao.observationCount)
        println(
            "10,000 items, index plus five searches: ${(System.nanoTime() - started) / 1_000_000} ms"
        )
    }

    @Test
    fun `one and two characters search captions and descriptions too`() = runTest {
        val search = observeSearch()
        search.dao.items.value =
            listOf(
                item(1, caption = "Funny"),
                item(2, description = "Fun afternoon"),
                item(3, tags = listOf("fun")),
            )
        for (query in listOf("f", "fu", "fun")) {
            search.query.value = query
            runCurrent()
            assertEquals(setOf(1L, 2L, 3L), search.ids.toSet())
        }
        // A four-character query may now match a one-edit typo.
        search.query.value = "funz"
        runCurrent()
        assertEquals(listOf(3L, 2L), search.ids)
    }

    private fun TestScope.observeSearch(): SearchFixture {
        val fixture = SearchFixture()
        val search =
            MediaSearchUseCase(
                MediaItemRepository(fixture.dao) { error("Fixture contains no images") },
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
        val ids
            get() = latest.map { it.id }
    }

    private fun item(
        id: Long,
        caption: String = "",
        description: String = "",
        tags: List<String> = emptyList(),
    ) =
        MediaItemDetails(
            mediaItem = MediaItemEntity(id, imageId = null, caption, description),
            tags = tags.mapIndexed { index, name -> TagEntity(index.toLong(), name) },
        )

    private class FakeMediaItemDao : MediaItemDao {
        val items = MutableStateFlow<List<MediaItemDetails>>(emptyList())
        val memberships = MutableStateFlow<Map<Long, Set<Long>>>(emptyMap())
        val activeScopes = mutableSetOf<Long?>()
        var observationCount = 0
        var failuresRemaining = 0
        var delays = emptyMap<Long?, Long>()

        override fun observeByCollection(collectionId: Long?): Flow<List<MediaItemDetails>> = flow {
            observationCount++
            if (failuresRemaining > 0) {
                failuresRemaining--
                error("Database unavailable")
            }
            activeScopes += collectionId
            try {
                delay(delays[collectionId] ?: 0L)
                emitAll(
                    combine(items, memberships) { items, memberships ->
                        items.filter {
                            collectionId == null ||
                                it.mediaItem.id in memberships[collectionId].orEmpty()
                        }
                    }
                )
            } finally {
                activeScopes -= collectionId
            }
        }

        override suspend fun insert(mediaItem: MediaItemEntity): Long = error("Unused")

        override suspend fun update(mediaItem: MediaItemEntity): Int = error("Unused")

        override suspend fun delete(mediaItemId: Long): Unit = error("Unused")

        override suspend fun deleteByIds(mediaItemIds: Set<Long>): Unit = error("Unused")

        override fun observeAll(): Flow<List<MediaItemDetails>> = error("Unused")

        override suspend fun get(mediaItemId: Long): MediaItemDetails? = error("Unused")

        override suspend fun getIdsByTags(tagIds: List<Long>, tagCount: Int): List<Long> =
            error("Unused")

        override suspend fun getByIds(mediaItemIds: List<Long>): List<MediaItemDetails> =
            error("Unused")

        override suspend fun getByCollection(collectionId: Long): List<MediaItemDetails> =
            error("Unused")

        override suspend fun getAllIds(): List<Long> = error("Unused")

        override suspend fun getImageId(mediaItemId: Long): Long? = error("Unused")

        override suspend fun getImageIds(mediaItemIds: Set<Long>): List<Long> = error("Unused")
    }
}
