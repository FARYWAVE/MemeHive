package com.farywave.memehive.ui.model

import com.farywave.memehive.domain.model.Image

data class MediaItemDraft(
    val id: Long = 0,
    val image: Image? = null,
    val caption: String = "",
    val description: String = "",
    val tags: List<EditableTag> = emptyList(),
    val collectionIds: Set<Long> = emptySet(),
) {
    /** Compares editable content, ignoring item/image IDs and tag keys, order, and duplicates. */
    fun hasSameContentAs(other: MediaItemDraft?): Boolean =
        other != null &&
            image?.uri == other.image?.uri &&
            caption == other.caption &&
            description == other.description &&
            tags.map { it.text }.toSet() == other.tags.map { it.text }.toSet() &&
            collectionIds == other.collectionIds

    data class EditableTag(
        val key: Int,
        val text: String,
    )
}
