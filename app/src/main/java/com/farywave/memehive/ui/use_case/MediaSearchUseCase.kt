package com.farywave.memehive.ui.use_case

import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.domain.model.MediaItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

class MediaSearchUseCase(
    private val mediaItemRepository: MediaItemRepository,
    private val dispatcher: CoroutineDispatcher,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        collectionIds: Flow<Long?>,
        queries: Flow<String>,
    ): Flow<List<MediaItem>> = collectionIds
        .distinctUntilChanged()
        .flatMapLatest { collectionId ->
            combine(
                mediaItemRepository.observeMediaItems(collectionId),
                queries.map { it.trim() }.distinctUntilChanged(),
            ) { items, query -> items to query }
                .mapLatest { (items, query) -> search(items, query) }
        }
        .distinctUntilChanged()
        .flowOn(dispatcher)

    private suspend fun search(items: List<MediaItem>, query: String): List<MediaItem> {
        if (query.isEmpty()) return items

        val context = currentCoroutineContext()
        return items.mapNotNull { item ->
            context.ensureActive()
            val rank = when {
                item.tags.any { it.startsWith(query, ignoreCase = true) } -> 0
                item.tags.any { it.contains(query, ignoreCase = true) } -> 1
                item.caption.contains(query, ignoreCase = true) -> 2
                item.description.contains(query, ignoreCase = true) -> 3
                else -> return@mapNotNull null
            }
            rank to item
        }.sortedBy { it.first }.map { it.second }
    }
}
