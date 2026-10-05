package com.farywave.memehive.ui.use_case

import androidx.room.withTransaction
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.ImageRepository
import com.farywave.memehive.ui.model.Collection

class CollectionUseCases(
    val db: MemeHiveDatabase,
    val collectionRepository: CollectionRepository,
    val imageRepository: ImageRepository,
) {
    suspend fun createCollection(collection: Collection): Collection {
        return db.withTransaction {
            val image = collection.cover?.let { cover ->
                imageRepository.insertOrGet(cover.uri)
            }
            val id = collectionRepository.insertCollection(collection.copy(cover = image))

            collection.copy(id = id, cover = image)
        }
    }

    suspend fun updateCollection(collection: Collection): Collection {
        return imageRepository.withImageCleanup { cleanup ->
            db.withTransaction {
                val oldCoverId =
                    collectionRepository.getCollectionCoverId(collection.id)
                val image = collection.cover?.let { cover ->
                    imageRepository.insertOrGet(cover.uri)
                }

                val updatedCollection = collection.copy(cover = image)
                collectionRepository.updateCollection(updatedCollection)

                cleanup(setOf(oldCoverId))

                updatedCollection
            }
        }
    }

    suspend fun deleteCollection(collection: Collection) {
        imageRepository.withImageCleanup { cleanup ->
            db.withTransaction {
                val coverId = collectionRepository.getCollectionCoverId(collection.id)
                collectionRepository.deleteCollection(collection)
                cleanup(setOf(coverId))
            }
        }
    }

    fun getAllCollections() = collectionRepository.getAllCollections()

    suspend fun addItemsToCollection(mediaItemIds: Set<Long>, collectionId: Long) =
        collectionRepository.insertCollectionEntries(mediaItemIds, collectionId)

    suspend fun getCollectionIdsByMediaItem(mediaItemId: Long) =
        collectionRepository.getCollectionIdsByMediaItem(mediaItemId)

    suspend fun updateMediaItemCollections(mediaItemId: Long, collectionIds: Set<Long>) {
        db.withTransaction {
            collectionRepository.deleteCollectionEntriesByItem(mediaItemId)
            collectionRepository.addItemToCollections(mediaItemId, collectionIds)
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
}