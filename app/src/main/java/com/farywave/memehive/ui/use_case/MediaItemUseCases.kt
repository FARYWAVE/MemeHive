package com.farywave.memehive.ui.use_case

import android.net.Uri
import androidx.room.withTransaction
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.ImageRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.data.local.db.repository.TagRepository
import com.farywave.memehive.data.local.storage.ImageChanges
import com.farywave.memehive.domain.model.MediaItem

class MediaItemUseCases(
    private val db: MemeHiveDatabase,
    private val mediaItemRepository: MediaItemRepository,
    private val tagRepository: TagRepository,
    private val collectionRepository: CollectionRepository,
    private val imageRepository: ImageRepository,
) {
    suspend fun getMediaItemById(id: Long): MediaItem? {
        return mediaItemRepository.getMediaItemById(id)
    }

    suspend fun getByCollectionId(collectionId: Long): List<MediaItem> {
        return mediaItemRepository.getMediaItemsByCollectionId(collectionId)
    }

    suspend fun getImageSharingUri(imageId: Long): Uri {
        return imageRepository.sharingUri(imageId)
    }

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

                cleanup(imageIds.minus(null))
            }
        }
    }
}
