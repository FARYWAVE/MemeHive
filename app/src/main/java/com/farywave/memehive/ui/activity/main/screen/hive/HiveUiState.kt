package com.farywave.memehive.ui.activity.main.screen.hive

import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.domain.model.MediaItem
import com.farywave.memehive.ui.activity.main.dialog.collection_editor.CollectionEditorState

data class HiveUiState(
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val searchQuery: String = "",
    val mediaItems: List<MediaItem> = emptyList(),
    val selectedMediaItemIds: Set<Long> = emptySet(),
    val collections: List<Collection> = emptyList(),
    val selectedCollectionId: Long? = null,
    val isSelectionMode: Boolean = false,
    val collectionEditor: CollectionEditorState? = null,
)
