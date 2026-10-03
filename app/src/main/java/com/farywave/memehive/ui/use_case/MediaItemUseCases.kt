package com.farywave.memehive.ui.use_case

import com.farywave.memehive.core.ModelTools
import com.farywave.memehive.data.local.db.relation.MediaItemDetails
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.ImageRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.data.local.db.repository.TagRepository
import com.farywave.memehive.data.local.db.repository.TrigramRepository
import com.farywave.memehive.ui.model.MediaItem

class MediaItemUseCases(
    val mediaItemRepository: MediaItemRepository,
    val tagRepository: TagRepository,
    val collectionRepository: CollectionRepository,
    val trigramRepository: TrigramRepository,
    val imageRepository: ImageRepository,
) {
    suspend fun createMediaItem(
        mediaItem: MediaItem,
        collectionsIDsToAdd: Set<Long> = emptySet()
    ): Long {
        val image = mediaItem.image?.let {
            if (it.id == 0L) imageRepository.insertOrGet(it.uri) else it
        }

        val mediaItemID = mediaItemRepository.insertMediaItem(mediaItem.copy(id = 0, image = image))

        mediaItem.tags.forEach { tag ->
            tagRepository.insert(tag, mediaItemID)
        }

        collectionRepository.addItemToCollections(mediaItemID, collectionsIDsToAdd)

        val trigrams = ModelTools.generateTrigrams(mediaItem.caption)
            .union(ModelTools.generateTrigrams(mediaItem.description))
        trigramRepository.insertAll(trigrams, mediaItemID)

        return mediaItemID
    }

    suspend fun updateMediaItem(mediaItem: MediaItem) {
        val oldMediaItem = mediaItemRepository.getMediaItemById(mediaItem.id)

        val image = mediaItem.image?.let { imageRepository.insertOrGet(it.uri) }
        val updatedMediaItem = mediaItem.copy(image = image)
        oldMediaItem?.image?.let { imageRepository.deleteIfUnused(it.id) }

        mediaItemRepository.updateMediaItem(updatedMediaItem)

        tagRepository.updateTagsForMediaItem(updatedMediaItem)

        trigramRepository.updateTrigramsForMediaItem(
            updatedMediaItem.id,
            ModelTools.generateTrigrams(updatedMediaItem.caption)
                .union(ModelTools.generateTrigrams(updatedMediaItem.description))
        )

    }

    suspend fun deleteMediaItem(mediaItem: MediaItem) {
        mediaItem.image?.let { imageRepository.deleteIfUnused(it.id) }
        mediaItemRepository.deleteMediaItem(mediaItem)
        tagRepository.deleteOrphanedTags()
    }

    suspend fun deleteMediaItemsByIDs(mediaItemIDs: Set<Long>) =
        mediaItemRepository.deleteMediaItemsByIDs(mediaItemIDs)

    suspend fun getMediaItemByID(id: Long) = mediaItemRepository.getMediaItemById(id)

    suspend fun getByCollectionID(collectionID: Long) =
        mediaItemRepository.getMediaItemsByCollectionID(collectionID)
}