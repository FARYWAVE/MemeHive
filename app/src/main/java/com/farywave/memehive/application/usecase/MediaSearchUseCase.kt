package com.farywave.memehive.application.usecase

import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.domain.model.MediaItem
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart

data class MediaSearchState(
    val items: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val failure: Throwable? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MediaSearchUseCase(
    private val mediaItemRepository: MediaItemRepository,
    private val dispatcher: CoroutineDispatcher,
) {
    fun observeState(
        collectionIds: Flow<Long?>,
        queries: Flow<String>,
        retries: Flow<Int>,
    ): Flow<MediaSearchState> =
        collectionIds.distinctUntilChanged().flatMapLatest { id ->
            retries.flatMapLatest {
                invoke(flowOf(id), queries)
                    .map { MediaSearchState(items = it) }
                    .onStart { emit(MediaSearchState(isLoading = true)) }
                    .catch {
                        if (it is CancellationException) throw it
                        emit(MediaSearchState(failure = it))
                    }
            }
        }

    operator fun invoke(collectionIds: Flow<Long?>, queries: Flow<String>): Flow<List<MediaItem>> =
        collectionIds
            .distinctUntilChanged()
            .flatMapLatest { collectionId ->
                val index =
                    mediaItemRepository.observeMediaItems(collectionId).mapLatest { items ->
                        items.map { item ->
                            currentCoroutineContext().ensureActive()
                            SearchDocument(item)
                        }
                    }
                combine(index, queries.map(::normalize).distinctUntilChanged()) { items, query ->
                        items to query
                    }
                    .mapLatest { (items, query) -> search(items, query) }
            }
            .distinctUntilChanged()
            .flowOn(dispatcher)
            .buffer(0)

    private suspend fun search(items: List<SearchDocument>, query: String): List<MediaItem> {
        if (query.isEmpty()) return items.map { it.item }
        val context = currentCoroutineContext()
        val fuzzy = query.length >= 4 && query.all { it.isLetterOrDigit() }
        return items
            .mapNotNull { doc ->
                context.ensureActive()
                val rank =
                    when {
                        query in doc.tags -> 0
                        doc.tags.any { it.startsWith(query) } -> 1
                        doc.tags.any { it.contains(query) } -> 2
                        doc.caption.contains(query) -> 3
                        doc.description.contains(query) -> 4
                        else -> 5
                    }
                if (rank < 5) Match(doc.item, rank, 1.0)
                else if (fuzzy) {
                    var score = 0.0
                    for ((field, words) in doc.words.withIndex()) {
                        for (word in words) {
                            context.ensureActive()
                            if (oneEditApart(query, word)) {
                                score =
                                    maxOf(
                                        score,
                                        (1.0 - 1.0 / maxOf(query.length, word.length)) * (3 - field),
                                    )
                            }
                        }
                    }
                    if (score > 0) Match(doc.item, rank, score) else null
                } else null
            }
            .sortedWith(
                compareBy<Match> { it.rank }.thenByDescending { it.score }.thenBy { it.item.id }
            )
            .map { it.item }
    }

    private data class Match(val item: MediaItem, val rank: Int, val score: Double)

    private class SearchDocument(val item: MediaItem) {
        val tags = item.tags.map(::normalize)
        val caption = normalize(item.caption)
        val description = normalize(item.description)
        val words =
            listOf(
                tags.flatMap(::words).toSet(),
                words(caption).toSet(),
                words(description).toSet(),
            )
    }

    companion object {
        private val whitespace = Regex("\\s+")
        private val separators = Regex("[^\\p{L}\\p{N}]+")

        private fun normalize(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFKC)
                .lowercase(Locale.ROOT)
                .replace(whitespace, " ")
                .trim()

        private fun words(text: String): List<String> =
            text.split(separators).filter { it.isNotEmpty() }

        private fun oneEditApart(a: String, b: String): Boolean {
            if (kotlin.math.abs(a.length - b.length) > 1) return false
            var i = 0
            while (i < minOf(a.length, b.length) && a[i] == b[i]) i++
            if (i == minOf(a.length, b.length)) return a.length != b.length
            return when {
                a.length == b.length ->
                    a.regionMatches(i + 1, b, i + 1, a.length - i - 1) ||
                        (i + 1 < a.length &&
                            a[i] == b[i + 1] &&
                            a[i + 1] == b[i] &&
                            a.regionMatches(i + 2, b, i + 2, a.length - i - 2))
                a.length > b.length -> a.regionMatches(i + 1, b, i, b.length - i)
                else -> a.regionMatches(i, b, i + 1, a.length - i)
            }
        }
    }
}
