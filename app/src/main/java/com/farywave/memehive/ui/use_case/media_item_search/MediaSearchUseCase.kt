package com.farywave.memehive.ui.use_case.media_item_search

import com.farywave.memehive.core.ModelTools
import com.farywave.memehive.data.local.db.dao.MediaItemDao
import com.farywave.memehive.data.local.db.relation.MediaItemDetails
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.TagRepository
import com.farywave.memehive.data.local.db.repository.TrigramRepository
import com.farywave.memehive.ui.model.MediaItem

class MediaSearchUseCase(
    private val mediaItemDao: MediaItemDao,
    private val tagRepository: TagRepository,
    private val trigramRepository: TrigramRepository,
    private val collectionRepository: CollectionRepository
) {

    suspend operator fun invoke(
        collectionId: Long?,
        query: String?
    ): List<MediaItem> {

        val baseIds = getSearchScope(collectionId)

        val entities =
            mediaItemDao.getByIds(baseIds.toList())

        if (query.isNullOrBlank()) {
            return entities.map { it.toMediaItem() }
        }

        val normalizedQuery = query.lowercase()

        val matches = findMatches(
            query = normalizedQuery,
            entities = entities
        )

        val matchedIds =
            collectMatchedIds(
                matches = matches,
                baseIds = baseIds,
                isShortQuery = normalizedQuery.length < 3
            )

        return entities
            .filter { it.mediaItem.id in matchedIds }
            .sortedBy { calculateScore(it.mediaItem.id, matches) }
            .map { it.toMediaItem() }
    }

    private suspend fun getSearchScope(
        collectionId: Long?
    ): Set<Long> {

        return if (collectionId == null || collectionId == -1L) {
            mediaItemDao.getAllIDs().toSet()
        } else {
            mediaItemDao.getByCollection(collectionId).map { it.mediaItem.id }.toSet()
        }
    }

    private suspend fun findMatches(
        query: String,
        entities: List<MediaItemDetails>
    ): SearchResult {

        val prefixMatches = mutableSetOf<Long>()
        val containsMatches = mutableSetOf<Long>()

        entities.forEach { entity ->
            entity.tags.forEach { tag ->

                val name = tag.name.lowercase()

                when {
                    name.startsWith(query) ->
                        prefixMatches += entity.mediaItem.id

                    name.contains(query) ->
                        containsMatches += entity.mediaItem.id
                }
            }
        }

        val trigramMatches = searchByTrigrams(query)

        val tagMatches = searchByFuzzyTags(query)

        return SearchResult(
            prefixMatches = prefixMatches,
            containsMatches = containsMatches,
            tagMatches = tagMatches,
            trigramMatches = trigramMatches
        )
    }

    private suspend fun searchByTrigrams(
        query: String
    ): Set<Long> {

        if (query.length < 3) {
            return emptySet()
        }

        val trigrams = ModelTools.generateTrigrams(query)

        if (trigrams.isEmpty()) {
            return emptySet()
        }

        return trigramRepository.getMediaItemIDsByTrigrams(trigrams).toSet()
    }

    private suspend fun searchByFuzzyTags(
        query: String
    ): Set<Long> {

        val tags = searchTags(query)

        if (tags.isEmpty()) {
            return emptySet()
        }

        val tagIds =
            tagRepository.getIDsByNames(tags)

        if (tagIds.isEmpty()) {
            return emptySet()
        }

        return tagRepository.getMediaItemIDsByTagIDs(tagIds).toSet()
    }

    private fun collectMatchedIds(
        matches: SearchResult,
        baseIds: Set<Long>,
        isShortQuery: Boolean
    ): Set<Long> {

        val result =
            (matches.prefixMatches +
                    matches.containsMatches +
                    matches.tagMatches +
                    matches.trigramMatches)
                .intersect(baseIds)

        if (result.isNotEmpty()) {
            return result
        }

        return if (isShortQuery) {
            (matches.containsMatches + matches.tagMatches)
                .intersect(baseIds)
        } else {
            emptySet()
        }
    }

    private fun calculateScore(
        mediaId: Long,
        matches: SearchResult
    ): Int {

        return when {
            mediaId in matches.prefixMatches -> 0
            mediaId in matches.containsMatches -> 1
            mediaId in matches.tagMatches -> 2
            mediaId in matches.trigramMatches -> 3
            else -> Int.MAX_VALUE
        }
    }

    private suspend fun searchTags(query: String, maxDistance: Int = 2): List<String> {
        fun normalize(s: String) = s.lowercase().trim()
        fun levenshtein(a: String, b: String): Int {
            val dp = Array(a.length + 1) { IntArray(b.length + 1) }
            for (i in 0..a.length) dp[i][0] = i
            for (j in 0..b.length) dp[0][j] = j
            for (i in 1..a.length) {
                for (j in 1..b.length) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                }
            }
            return dp[a.length][b.length]
        }

        val normalizedQuery = normalize(query)
        return tagRepository.getAll().asSequence().map { it.name }
            .map { tag -> tag to levenshtein(normalizedQuery, normalize(tag)) }
            .filter { (_, distance) -> distance <= maxDistance }.sortedBy { it.second }
            .map { it.first }.toList()
    }
}