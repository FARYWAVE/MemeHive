package com.farywave.memehive.core.collection_transfer.entity

import kotlinx.serialization.Serializable

@Serializable
data class TransferMediaItem(
    val path: String?,
    val caption: String,
    val description: String,
    val tags: List<String>,
)
