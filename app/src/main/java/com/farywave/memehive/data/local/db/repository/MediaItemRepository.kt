package com.farywave.memehive.data.local.db.repository

import androidx.room.Transaction
import com.farywave.memehive.data.local.db.dao.MediaItemDao
import com.farywave.memehive.data.local.db.dao.MediaItemTagDao
import com.farywave.memehive.data.local.db.dao.MediaItemTrigramDao
import com.farywave.memehive.data.local.db.dao.TagDao
import com.farywave.memehive.data.local.db.entity.MediaItemTagEntity
import com.farywave.memehive.data.local.db.entity.MediaItemTrigramEntity
import com.farywave.memehive.data.local.db.entity.TagEntity
import com.farywave.memehive.ui.model.MediaItem
import com.farywave.memehive.ui.model.Tag
import kotlinx.coroutines.flow.map

class MediaItemRepository(
    private val mediaItemDao: MediaItemDao,
    private val mediaItemTagDao: MediaItemTagDao,
    private val tagDao: TagDao,
    private val mediaItemTrigramDao: MediaItemTrigramDao,
) {
    @Transaction
    suspend fun insertMediaItem(mediaItem: MediaItem) {
        mediaItem.id  = mediaItemDao.insertMediaItem(
            mediaItem.toMediaItemEntity()
        )

        mediaItem.tags.forEach {
            val tagId = resolveTagId(it.name)

            mediaItemTagDao.insertMediaItemTag(
                MediaItemTagEntity(mediaItem.id, tagId)
            )
        }

        insertTrigrams(mediaItem)
    }

    private suspend fun insertTrigrams(mediaItem: MediaItem) {
        val text = buildString {
            append(mediaItem.name ?: "")
            append(" ")
            append(mediaItem.description ?: "")
            append(" ")
            mediaItem.tags.forEach { append(it.name).append(" ") }
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
            val tagId = resolveTagId(it.name)
            mediaItemTagDao.insertMediaItemTag(
                MediaItemTagEntity(mediaItem.id, tagId)
            )
        }

        insertTrigrams(mediaItem)
    }

    @Transaction
    suspend fun deleteMediaItem(mediaItem: MediaItem) =
        mediaItemDao.deleteMediaItem(mediaItem.toMediaItemEntity())

    fun observeMediaItems() = mediaItemDao.getAllMediaItems().map { list ->
        list.map { it.toMediaItem() }
    }

    suspend fun search(
        collectionId: Long?,
        query: String?,
    ) {
        val trigrams = query?.let { generateTrigrams(it) }
        val tags = query?.let { searchTags(it) }

        mediaItemDao.searchMedia(
            collectionId,
            trigrams,
            tags?.map { it.id }?.toSet()
        )
    }

    private suspend fun resolveTagId(name: String): Long {
        return tagDao.getTagByName(name)?.id
            ?: tagDao.insertTag(TagEntity(name = name))
    }

    private fun generateTrigrams(text: String): Set<String> {
        val normalized = text.lowercase()
        val result = mutableSetOf<String>()

        for (i in 0..normalized.length - 3) {
            result.add(normalized.substring(i, i + 3))
        }

        return result
    }

    private suspend fun searchTags(query: String, maxDistance: Int = 2): List<Tag> {
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
            .map { it.toTag() }
            .map { tag ->
                tag to levenshtein(normalizedQuery, normalize(tag.name))
            }
            .filter { (_, distance) -> distance <= maxDistance }
            .sortedBy { it.second }
            .map { it.first }
    }
}