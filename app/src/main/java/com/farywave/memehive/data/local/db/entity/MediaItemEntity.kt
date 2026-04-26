package com.farywave.memehive.data.local.db.entity

import com.farywave.memehive.ui.model.MediaItem

data class MediaItemEntity(
    val id: Int,
    var url: String?,
    var name: String?,
    var description: String?,
    val tags: List<String>,
    var isSelected: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        return other is MediaItemEntity && other.id == id
    }

    fun toMediaItem() = MediaItem(id, url, name, description, tags, isSelected)
}