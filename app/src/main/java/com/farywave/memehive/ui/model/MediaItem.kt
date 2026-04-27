package com.farywave.memehive.ui.model

import com.farywave.memehive.data.local.db.entity.MediaItemEntity

data class MediaItem(
    val id: Int,
    var url: String?,
    var name: String?,
    var description: String?,
    val tags: List<String>,
    var isSelected: Boolean = false,
) {

    fun toMediaItemEntity() = MediaItemEntity(id, url, name, description, tags, isSelected)
}