package com.farywave.memehive.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.Image
import java.io.File

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "cover_image_id")
    val coverImageId: Long?,
) {
    fun toModel(cover: Image, mediaItemCount: Int) = Collection(
        id = id,
        name = name,
        cover = cover,
        mediaItemCount = mediaItemCount
    )
}