package com.farywave.memehive.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.farywave.memehive.ui.model.MediaItem

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val id: Long,
    val url: String?,
    val name: String?,
    val description: String?,
) {
    fun toMediaItem() = MediaItem(id, url, name, description, mutableListOf())
}