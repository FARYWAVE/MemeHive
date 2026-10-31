package com.farywave.memehive.ui.use_case

import androidx.room.withTransaction
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.data.local.db.repository.ImageRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.data.local.db.repository.TagRepository

class DeleteMediaItemsUseCase(
    private val db: MemeHiveDatabase,
    private val mediaItemRepository: MediaItemRepository,
    private val tagRepository: TagRepository,
    private val imageRepository: ImageRepository,
) {
    suspend operator fun invoke(mediaItemIds: Set<Long>) {
        imageRepository.withImageCleanup { cleanup ->
            db.withTransaction {
                invoke(mediaItemIds, cleanup)
            }
        }
    }

    internal suspend operator fun invoke(
        mediaItemIds: Set<Long>,
        cleanup: suspend (Set<Long?>) -> Unit,
    ) {
        val imageIds = mediaItemRepository.getMediaItemImageIds(mediaItemIds)
        mediaItemRepository.deleteMediaItemsByIds(mediaItemIds)
        tagRepository.deleteOrphanedTags()
        cleanup(imageIds.minus(null))
    }
}
