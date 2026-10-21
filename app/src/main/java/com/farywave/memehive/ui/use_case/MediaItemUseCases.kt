package com.farywave.memehive.ui.use_case

import androidx.room.withTransaction
import com.farywave.memehive.core.ModelTools
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.ImageRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.data.local.db.repository.TagRepository
import com.farywave.memehive.data.local.db.repository.TrigramRepository
import com.farywave.memehive.data.local.storage.ImageChanges
import com.farywave.memehive.domain.model.MediaItem

class MediaItemUseCases(
    private val db: MemeHiveDatabase,
    private val mediaItemRepository: MediaItemRepository,
    private val tagRepository: TagRepository,
    private val collectionRepository: CollectionRepository,
    private val trigramRepository: TrigramRepository,
    private val imageRepository: ImageRepository,
) {
    suspend fun createMediaItem(
        mediaItem: MediaItem,
        collectionIdsToAdd: Set<Long> = emptySet()
    ): MediaItem {
        return imageRepository.withImageChanges { changes ->
            createMediaItem(mediaItem, changes, collectionIdsToAdd)
        }
    }

    /** Use inside imageRepository.withImageChanges, sharing the tracker for the whole transaction. */
    suspend fun createMediaItem(
        mediaItem: MediaItem,
        changes: ImageChanges,
        collectionIdsToAdd: Set<Long> = emptySet(),
    ): MediaItem {
        return db.withTransaction {
            val image = mediaItem.image?.let {
                if (it.id == 0L) imageRepository.insertOrGet(it.uri, changes) else it
            }

            val mediaItemId =
                mediaItemRepository.insertMediaItem(mediaItem.copy(id = 0, image = image))

            mediaItem.tags.forEach { tag ->
                tagRepository.insert(tag, mediaItemId)
            }

            collectionRepository.addItemToCollections(mediaItemId, collectionIdsToAdd)

            val trigrams = ModelTools.generateTrigrams(mediaItem.caption)
                .union(ModelTools.generateTrigrams(mediaItem.description))
            trigramRepository.insertAll(trigrams, mediaItemId)

            mediaItem.copy(id = mediaItemId, image = image)
        }
    }

    suspend fun updateMediaItem(mediaItem: MediaItem, replacementCollectionIds: Set<Long> = emptySet()): MediaItem {
        return imageRepository.withImageChanges { changes ->
            imageRepository.withImageCleanup { cleanup ->
                db.withTransaction {
                    val oldImageId = mediaItemRepository.getMediaItemImageId(mediaItem.id)

                    val image = mediaItem.image?.let { imageRepository.insertOrGet(it.uri, changes) }
                    val updatedMediaItem = mediaItem.copy(image = image)

                    mediaItemRepository.updateMediaItem(updatedMediaItem)

                    tagRepository.updateTagsForMediaItem(updatedMediaItem)

                    trigramRepository.updateTrigramsForMediaItem(
                        updatedMediaItem.id,
                        ModelTools.generateTrigrams(updatedMediaItem.caption)
                            .union(ModelTools.generateTrigrams(updatedMediaItem.description))
                    )

                    collectionRepository.deleteCollectionEntriesByItem(mediaItem.id)
                    collectionRepository.addItemToCollections(mediaItem.id, replacementCollectionIds)

                    cleanup(setOf(oldImageId))

                    mediaItem.copy(image = image)
                }
            }
        }
    }

    suspend fun deleteMediaItem(mediaItem: MediaItem) {
        imageRepository.withImageCleanup { cleanup ->
            db.withTransaction {
                val imageId = mediaItemRepository.getMediaItemImageId(mediaItem.id)

                mediaItemRepository.deleteMediaItem(mediaItem)
                tagRepository.deleteOrphanedTags()

                cleanup(setOf(imageId))
            }
        }
    }

    suspend fun deleteMediaItemsByIds(mediaItemIds: Set<Long>) {
        imageRepository.withImageCleanup { cleanup ->
            db.withTransaction {
                val imageIds = mediaItemRepository.getMediaItemImageIds(mediaItemIds)
                mediaItemRepository.deleteMediaItemsByIds(mediaItemIds)
                tagRepository.deleteOrphanedTags()

                cleanup(imageIds)
            }
        }
    }

    suspend fun getImageSharingUri(imageId: Long) = imageRepository.sharingUri(imageId)

    suspend fun getMediaItemById(id: Long) = mediaItemRepository.getMediaItemById(id)

    suspend fun getByCollectionId(collectionId: Long) =
        mediaItemRepository.getMediaItemsByCollectionId(collectionId)
}
