package com.farywave.memehive.core.collection_transfer.entity

import kotlinx.serialization.Serializable


@Serializable
data class TransferManifest(
    val appVersion: Long,
    val exportedAt: Long,
    val collection: TransferCollection,
    val mediaItems: List<TransferMediaItem>
)