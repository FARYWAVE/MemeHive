package com.farywave.memehive.core.collection_transfer.mapper

import android.net.Uri
import com.farywave.memehive.core.collection_transfer.entity.TransferCollection
import com.farywave.memehive.core.collection_transfer.entity.TransferMediaItem
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.domain.model.MediaItem

fun TransferCollection.toCollection(cover: Uri?) =
    Collection(
        id = 0,
        name = name,
        cover = cover?.let { Image(uri = it) },
        mediaItemCount = 0,
    )

fun TransferMediaItem.toMediaItem(image: Uri?) =
    MediaItem(
        id = 0,
        image = image?.let { Image(uri = it) },
        caption = caption,
        description = description,
        tags = tags,
    )
