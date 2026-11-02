package com.farywave.memehive.data.local.db.mapper

import com.farywave.memehive.data.local.db.entity.MediaItemEntity
import com.farywave.memehive.data.local.db.relation.MediaItemDetails
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.domain.model.MediaItem

fun MediaItem.toEntity() =
    MediaItemEntity(
        id = id,
        imageId = image?.id,
        caption = caption,
        description = description,
    )

fun MediaItemDetails.toMediaItem(resolvedImage: Image?) =
    MediaItem(
        id = mediaItem.id,
        image = resolvedImage,
        caption = mediaItem.caption,
        description = mediaItem.description,
        tags = tags.map { it.name },
    )
