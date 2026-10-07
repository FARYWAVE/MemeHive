package com.farywave.memehive.domain.model

data class Collection(
    val id: Long = 0,
    val name: String,
    val cover: Image? = null,
    val mediaItemCount: Int = 0,
)
