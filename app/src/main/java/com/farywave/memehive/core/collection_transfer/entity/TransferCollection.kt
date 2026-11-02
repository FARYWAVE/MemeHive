package com.farywave.memehive.core.collection_transfer.entity

import kotlinx.serialization.Serializable

@Serializable
data class TransferCollection(
    val name: String,
    val coverPath: String?,
)
