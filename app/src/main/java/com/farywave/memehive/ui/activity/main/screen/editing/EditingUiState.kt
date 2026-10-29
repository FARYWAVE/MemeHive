package com.farywave.memehive.ui.activity.main.screen.editing

import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.ui.activity.main.dialog.collection_editor.CollectionEditorState
import com.farywave.memehive.ui.model.MediaItemDraft

data class EditingUiState(
    val mediaItemDraft: MediaItemDraft = MediaItemDraft(),
    val collections: List<Collection> = emptyList(),
    val collectionEditor: CollectionEditorState? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val loadError: Int? = null,
    val collectionsFailed: Boolean = false,
    val screenState: ScreenState = ScreenState.BLANK,
)
