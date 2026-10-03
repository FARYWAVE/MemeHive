package com.farywave.memehive.ui.model

data class MediaItemDraft(
    val id: Long = 0,
    val image: Image? = null,
    val caption: String = "",
    val description: String = "",
    val tags: List<EditableTag> = emptyList(),
    val collectionIds: Set<Long> = emptySet(),
) {
    data class EditableTag(
        val key: Int,
        val text: String
    )
}
