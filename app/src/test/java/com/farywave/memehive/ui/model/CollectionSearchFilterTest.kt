package com.farywave.memehive.ui.model

import com.farywave.memehive.application.usecase.MediaSearchState
import com.farywave.memehive.domain.model.MediaItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CollectionSearchFilterTest {
    @Test
    fun `switches immediately preserve ranking and do not restart search debounce`() = runTest {
        val items = listOf(MediaItem(id = 3), MediaItem(id = 1), MediaItem(id = 2))
        val query = MutableStateFlow("")
        val selection = MutableStateFlow<Long?>(null)
        val memberships = MutableStateFlow(mapOf(7L to setOf(1L, 3L), 8L to setOf(2L)))
        var subscriptions = 0
        val search = flow {
            subscriptions++
            emitAll(query.debounce(200).map { MediaSearchState(items = items) })
        }
        val states = mutableListOf<MediaSearchState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            search.filterByCollection(selection, memberships.asMembershipState()).collect {
                states += it
            }
        }
        advanceTimeBy(200)
        runCurrent()
        assertEquals(items, states.last().items)
        selection.value = 7
        runCurrent()
        assertEquals(listOf(3L, 1L), states.last().items.map { it.id })
        selection.value = 8
        runCurrent()
        assertEquals(listOf(2L), states.last().items.map { it.id })
        memberships.value = mapOf(8L to setOf(3L, 2L))
        runCurrent()
        assertEquals(listOf(3L, 2L), states.last().items.map { it.id })
        selection.value = 9
        runCurrent()
        assertTrue(states.last().items.isEmpty())
        selection.value = null
        runCurrent()
        assertEquals(items, states.last().items)
        assertTrue(states.none { it.isLoading })
        assertEquals(1, subscriptions)
        assertEquals(200L, testScheduler.currentTime)
    }

    @Test
    fun `pending failed and recovered membership never exposes other collections items`() = runTest {
        val item = MediaItem(id = 1)
        val search = MutableStateFlow(MediaSearchState(items = listOf(item)))
        val selection = MutableStateFlow<Long?>(7)
        val membership = MutableStateFlow(CollectionMembershipState())
        var latest = MediaSearchState()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            search.filterByCollection(selection, membership).collect { latest = it }
        }
        runCurrent()
        assertTrue(latest.isLoading)
        assertTrue(latest.items.isEmpty())
        val failure = IllegalStateException("Unavailable")
        membership.value = CollectionMembershipState(failure = failure)
        runCurrent()
        assertFalse(latest.isLoading)
        assertSame(failure, latest.failure)
        assertTrue(latest.items.isEmpty())
        selection.value = null
        runCurrent()
        assertEquals(search.value, latest)
        selection.value = 7
        membership.value = CollectionMembershipState(mapOf(7L to setOf(1L)))
        runCurrent()
        assertEquals(search.value, latest)
        search.value = search.value.copy(isLoading = true)
        runCurrent()
        assertTrue(latest.isLoading)
        assertEquals(listOf(item), latest.items)
    }
}
