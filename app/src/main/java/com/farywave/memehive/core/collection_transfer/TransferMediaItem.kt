package com.farywave.memehive.core.collection_transfer

import com.farywave.memehive.ui.model.MediaItem
import kotlinx.serialization.Serializable
import java.io.File


@Serializable
data class TransferMediaItem (
    val src: String?,
    val caption: String,
    val description: String,
    val tags: List<String>
) {
    fun toMediaItem(source: File?) = MediaItem(
        id = 0,
        src = source,
        caption = caption,
        description = description,
        tags = tags
    )
}