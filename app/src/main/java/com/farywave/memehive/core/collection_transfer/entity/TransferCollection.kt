package com.farywave.memehive.core.collection_transfer.entity

import android.net.Uri
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.Image
import kotlinx.serialization.Serializable


@Serializable
data class TransferCollection(
    val name: String,
    val coverPath: String?
) {
    fun toCollection(cover: Uri?) = Collection(
        id = 0,
        name = name,
        cover = cover?.let { Image(uri = it) },
        mediaItemCount = 0
    )
}