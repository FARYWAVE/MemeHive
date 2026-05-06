package com.farywave.memehive.data.local.db.repository

import androidx.room.Transaction
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.data.local.db.dao.CollectionEntryDao
import com.farywave.memehive.data.local.db.dao.MediaItemDao
import com.farywave.memehive.data.local.db.dao.MediaItemTagDao
import com.farywave.memehive.data.local.db.dao.MediaItemTrigramDao
import com.farywave.memehive.data.local.db.dao.TagDao
import com.farywave.memehive.data.local.db.entity.MediaItemTagEntity
import com.farywave.memehive.data.local.db.entity.MediaItemTrigramEntity
import com.farywave.memehive.data.local.db.entity.TagEntity
import com.farywave.memehive.data.local.db.relation.MediaWithTags
import com.farywave.memehive.ui.model.MediaItem
import kotlinx.coroutines.flow.map

class MediaItemRepository(
    private val mediaItemDao: MediaItemDao,
    private val mediaItemTagDao: MediaItemTagDao,
    private val tagDao: TagDao,
    private val mediaItemTrigramDao: MediaItemTrigramDao,
    private val collectionEntryDao: CollectionEntryDao,
) {
    @Transaction
    suspend fun insertMediaItem(mediaItem: MediaItem) {
        mediaItem.id = mediaItemDao.insertMediaItem(
            mediaItem.toMediaItemEntity().copy(id = 0)
        )

        mediaItem.tags.forEach {
            val tagId = resolveTagId(it)

            mediaItemTagDao.insertMediaItemTag(
                MediaItemTagEntity(mediaItem.id, tagId)
            )
        }

        insertTrigrams(mediaItem)
    }

    suspend fun getMediaItemById(id: Long): MediaItem? {
        return mediaItemDao.getMediaWithTagsById(id)?.toMediaItem()
    }

    private suspend fun insertTrigrams(mediaItem: MediaItem) {
        val text = buildString {
            append(mediaItem.caption ?: "")
            append(" ")
            append(mediaItem.description ?: "")
            append(" ")
            mediaItem.tags.forEach { append(it).append(" ") }
        }

        val trigrams = generateTrigrams(text)

        trigrams.forEach {
            mediaItemTrigramDao.insertMediaItemTrigram(
                MediaItemTrigramEntity(0, it, mediaItem.id)
            )
        }
    }

    @Transaction
    suspend fun updateMediaItem(mediaItem: MediaItem) {
        mediaItemDao.updateMediaItem(mediaItem.toMediaItemEntity())

        mediaItemTagDao.deleteTagsForMedia(mediaItem.id)
        mediaItemTrigramDao.deleteTrigramsForMedia(mediaItem.id)

        mediaItem.tags.forEach {
            val tagId = resolveTagId(it)
            mediaItemTagDao.insertMediaItemTag(
                MediaItemTagEntity(mediaItem.id, tagId)
            )
        }

        insertTrigrams(mediaItem)
    }

    @Transaction
    suspend fun deleteMediaItem(mediaItem: MediaItem) {
        mediaItemDao.deleteMediaItem(mediaItem.toMediaItemEntity())
        mediaItem.src?.let { DeviceTools.deleteFromInternalStorage(it) }
    }

    fun observeMediaItems() = mediaItemDao.getAllMediaItems().map { list ->
        list.map { it.toMediaItem() }
    }

    suspend fun search(
        collectionId: Long?,
        query: String?
    ): List<MediaItem> {

        val baseIds: Set<Long> = if (collectionId == null || collectionId == -1L) {
            mediaItemDao.getAllIds().toSet()
        } else {
            collectionEntryDao.getMediaIds(collectionId).toSet()
        }

        val entities = mediaItemDao.getMediaWithTagsByIds(baseIds.toList())

        if (query.isNullOrBlank()) {
            return entities.map { it.toMediaItem() }
        }

        val q = query.lowercase()
        val isShort = q.length < 3

        val prefixMatches = mutableSetOf<Long>()
        val containsMatches = mutableSetOf<Long>()

        entities.forEach { entity ->
            entity.tags.forEach { tag ->
                val name = tag.name.lowercase()

                when {
                    name.startsWith(q) -> prefixMatches.add(entity.media.id)
                    name.contains(q) -> containsMatches.add(entity.media.id)
                }
            }
        }

        val trigramIds: Set<Long> = if (!isShort) {
            val trigrams = generateTrigrams(q)
            if (trigrams.isNotEmpty()) {
                mediaItemTrigramDao.searchByTrigrams(trigrams.toList()).toSet()
            } else emptySet()
        } else {
            emptySet()
        }

        val tags = searchTags(q)
        val tagIds = if (tags.isNotEmpty()) {
            tagDao.getTagsByNames(tags).map { it.id }
        } else emptyList()

        val tagMatchedIds: Set<Long> = if (tagIds.isNotEmpty()) {
            mediaItemDao.getMediaIdsByTags(tagIds, tagIds.size).toSet()
        } else emptySet()

        val allMatchedIds: Set<Long> =
            (prefixMatches +
                    containsMatches +
                    tagMatchedIds +
                    trigramIds)
                .intersect(baseIds)
                .ifEmpty {
                    if (isShort) {
                        (containsMatches + tagMatchedIds).intersect(baseIds)
                    } else emptySet()
                }

        if (allMatchedIds.isEmpty()) return emptyList()

        fun score(entity: MediaWithTags): Int {
            val id = entity.media.id

            return when {
                id in prefixMatches -> 0
                id in containsMatches -> 1
                id in tagMatchedIds -> 2
                id in trigramIds -> 3
                else -> 100
            }
        }

        return entities
            .filter { it.media.id in allMatchedIds }
            .sortedBy { score(it) }
            .map { it.toMediaItem() }
    }

    private suspend fun resolveTagId(name: String): Long {
        return tagDao.getTagByName(name)?.id
            ?: tagDao.insertTag(TagEntity(name = name))
    }

    private fun generateTrigrams(text: String): Set<String> {
        val normalized = text.lowercase()
        if (normalized.length < 3) return emptySet()
        val result = mutableSetOf<String>()

        for (i in 0..normalized.length - 3) {
            result.add(normalized.substring(i, i + 3))
        }

        return result
    }

    private suspend fun searchTags(query: String, maxDistance: Int = 2): List<String> {
        fun normalize(s: String) =
            s.lowercase().trim()

        fun levenshtein(a: String, b: String): Int {
            val dp = Array(a.length + 1) { IntArray(b.length + 1) }

            for (i in 0..a.length) dp[i][0] = i
            for (j in 0..b.length) dp[0][j] = j

            for (i in 1..a.length) {
                for (j in 1..b.length) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1

                    dp[i][j] = minOf(
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + cost
                    )
                }
            }

            return dp[a.length][b.length]
        }

        val normalizedQuery = normalize(query)

        return tagDao.getAllTags()
            .map { it.name }
            .map { tag ->
                tag to levenshtein(normalizedQuery, normalize(tag))
            }
            .filter { (_, distance) -> distance <= maxDistance }
            .sortedBy { it.second }
            .map { it.first }
    }
}