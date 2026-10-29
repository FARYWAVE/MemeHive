package com.farywave.memehive.ui.activity.picker

import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.domain.model.MediaItem

data class PickerUiState(
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val searchQuery: String = "",
    val mediaItems: List<MediaItem> = emptyList(),
    val collections: List<Collection> = emptyList(),
    val selectedCollectionId: Long? = null,
)
