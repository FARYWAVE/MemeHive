package com.farywave.memehive.ui.model

import com.farywave.memehive.data.local.db.entity.MediaItemEntity

data class MediaItem(
    var id: Long = 0,
    var image: Image? = null,
    var caption: String = "",
    var description: String = "",
    val tags: List<String> = emptyList(),
    var isSelected: Boolean = false,
) {
    fun toEntity() = MediaItemEntity(
        id = id,
        imageID = image?.id,
        caption = caption,
        description = description,
    )
}