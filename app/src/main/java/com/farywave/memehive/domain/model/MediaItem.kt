package com.farywave.memehive.domain.model

data class MediaItem(
    val id: Long = 0,
    val image: Image? = null,
    val caption: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
)
