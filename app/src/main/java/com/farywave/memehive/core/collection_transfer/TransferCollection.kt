package com.farywave.memehive.core.collection_transfer

import com.farywave.memehive.ui.model.Collection
import kotlinx.serialization.Serializable
import java.io.File


@Serializable
data class TransferCollection(
    val name: String,
    val coverSrc: String?
) {
    fun toCollection(coverSrc: File?) = Collection(
        id = 0,
        name = name,
        cover = coverSrc,
        mediaItemCount = 0
    )
}