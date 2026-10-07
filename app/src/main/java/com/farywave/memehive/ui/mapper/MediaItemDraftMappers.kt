package com.farywave.memehive.ui.mapper

import com.farywave.memehive.domain.model.MediaItem
import com.farywave.memehive.ui.model.MediaItemDraft

fun MediaItem.toDraft(collectionIds: Set<Long>) = MediaItemDraft(
    id = id,
    image = image,
    caption = caption,
    description = description,
    tags = tags.mapIndexed { index, tag -> MediaItemDraft.EditableTag(index, tag) },
    collectionIds = collectionIds
)

fun MediaItemDraft.toMediaItem() = MediaItem(
    id = id,
    image = image,
    caption = caption,
    description = description,
    tags = tags.map { it.text }
)
