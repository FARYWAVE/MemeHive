package com.farywave.memehive.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "collection_entries",
    primaryKeys = ["collection_id", "media_item_id"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CollectionEntryEntity(
    @ColumnInfo(name = "collection_id")
    val collectionId: Long,

    @ColumnInfo(name = "media_item_id")
    val mediaItemId: Long
)