package com.farywave.memehive.application.usecase

import androidx.room.withTransaction
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.ImageRepository
import com.farywave.memehive.data.local.storage.ImageChanges
import com.farywave.memehive.domain.model.Collection
import kotlinx.coroutines.flow.Flow

class CollectionUseCases(
    private val db: MemeHiveDatabase,
    private val collectionRepository: CollectionRepository,
    private val imageRepository: ImageRepository,
    private val deleteMediaItemsUseCase: DeleteMediaItemsUseCase,
) {
    fun observeCollections(): Flow<List<Collection>> {
        return collectionRepository.observeCollections()
    }

    suspend fun getCollectionIdsByMediaItem(mediaItemId: Long): Set<Long> {
        return collectionRepository.getCollectionIdsByMediaItem(mediaItemId)
    }

    suspend fun createCollection(collection: Collection): Collection {
        return imageRepository.withImageChanges { changes ->
            createCollection(collection, changes)
        }
    }

    suspend fun createCollection(collection: Collection, changes: ImageChanges): Collection {
        return db.withTransaction {
            val image =
                collection.cover?.let { cover ->
                    imageRepository.insertOrGet(cover.uri, changes)
                }
            val id = collectionRepository.insertCollection(collection.copy(cover = image))

            collection.copy(id = id, cover = image)
        }
    }

    suspend fun addItemsToCollection(mediaItemIds: Set<Long>, collectionId: Long): List<Long> {
        return collectionRepository.insertCollectionEntries(mediaItemIds, collectionId)
    }

    suspend fun updateCollection(collection: Collection): Collection {
        return imageRepository.withImageChanges { changes ->
            imageRepository.withImageCleanup { cleanup ->
                db.withTransaction {
                    val oldCoverId = collectionRepository.getCollectionCoverId(collection.id)
                    val image =
                        collection.cover?.let { cover ->
                            imageRepository.insertOrGet(cover.uri, changes)
                        }

                    val updatedCollection = collection.copy(cover = image)
                    collectionRepository.updateCollection(updatedCollection)

                    cleanup(setOf(oldCoverId))

                    updatedCollection
                }
            }
        }
    }

    suspend fun setCollectionCover(collectionId: Long, imageId: Long) {
        imageRepository.withImageCleanup { cleanup ->
            db.withTransaction {
                val oldCoverId = collectionRepository.getCollectionCoverId(collectionId)
                collectionRepository.setCollectionCover(collectionId, imageId)
                cleanup(setOf(oldCoverId))
            }
        }
    }

    /** [deleteContents] deletes only items that belong to no other collection. */
    suspend fun deleteCollection(collection: Collection, deleteContents: Boolean = false) {
        imageRepository.withImageCleanup { cleanup ->
            db.withTransaction {
                val coverId = collectionRepository.getCollectionCoverId(collection.id)
                if (deleteContents) {
                    val itemIds =
                        collectionRepository.getExclusiveMediaItemIdsByCollection(collection.id)
                    deleteMediaItemsUseCase(itemIds, cleanup)
                }
                collectionRepository.deleteCollection(collection)
                cleanup(setOf(coverId))
            }
        }
    }
}
