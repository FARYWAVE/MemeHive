package com.farywave.memehive.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "collection_entries",
    primaryKeys = ["collectionId", "mediaItemId"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CollectionEntryEntity(
    val collectionId: Long,
    val mediaItemId: Long
)