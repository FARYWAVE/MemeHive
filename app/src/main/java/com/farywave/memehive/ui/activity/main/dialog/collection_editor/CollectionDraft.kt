package com.farywave.memehive.ui.activity.main.dialog.collection_editor

import androidx.annotation.StringRes
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class CollectionDraft(
    val name: String = "",
    val coverUri: String? = null,
)

@Serializable
data class CollectionEditorState(
    val sessionId: String,
    val collectionId: Long? = null,
    val initialDraft: CollectionDraft = CollectionDraft(),
    @Transient val isSaving: Boolean = false,
    @Transient @param:StringRes val errorResId: Int? = null,
)
