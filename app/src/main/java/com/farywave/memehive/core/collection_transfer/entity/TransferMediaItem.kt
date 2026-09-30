package com.farywave.memehive.core.collection_transfer.entity

import android.net.Uri
import com.farywave.memehive.ui.model.Image
import com.farywave.memehive.ui.model.MediaItem
import kotlinx.serialization.Serializable


@Serializable
data class TransferMediaItem (
    val path: String?,
    val caption: String,
    val description: String,
    val tags: List<String>
) {
    fun toMediaItem(image: Uri?) = MediaItem(
        id = 0,
        image = image?.let { Image(uri = it) },
        caption = caption,
        description = description,
        tags = tags
    )
}