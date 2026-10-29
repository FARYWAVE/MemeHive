package com.farywave.memehive.ui.activity.main.screen.hive

import com.farywave.memehive.ui.collection.CollectionEditorState
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.domain.model.MediaItem


data class HiveUiState(
    val searchQuery: String = "",
    val mediaItems: List<MediaItem> = emptyList(),
    val selectedMediaItemIds: Set<Long> = emptySet(),
    val collections: List<Collection> = emptyList(),
    val selectedCollectionId: Long? = null,
    val isMassEditingMode: Boolean = false,
    val collectionEditor: CollectionEditorState? = null
)
