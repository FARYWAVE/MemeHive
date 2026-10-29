package com.farywave.memehive.ui.activity.main.dialog.collection_editor

import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.farywave.memehive.R
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.ui.use_case.CollectionUseCases
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


class CollectionEditorViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val saveCollection: suspend (Long?, CollectionDraft) -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(
        savedStateHandle.get<String>(STATE_KEY)?.let {
            Json.decodeFromString<CollectionEditorState>(it)
        }
    )
    val state = _state.asStateFlow()

    fun open(collection: Collection? = null) {
        if (_state.value != null) return
        setState(
            CollectionEditorState(
                sessionId = UUID.randomUUID().toString(),
                collectionId = collection?.id,
                initialDraft = CollectionDraft(collection?.name.orEmpty(), collection?.cover?.uri?.toString()),
            )
        )
    }

    fun dismiss() {
        if (_state.value?.isSaving != true) setState(null)
    }

    fun submit(draft: CollectionDraft) {
        val editor = _state.value ?: return
        if (editor.isSaving) return
        val submitted = draft.copy(name = draft.name.trim())
        if (submitted.name.isEmpty()) {
            setState(editor.copy(initialDraft = draft, errorResId = R.string.error_collection_name_required))
            return
        }


        setState(editor.copy(initialDraft = draft, isSaving = true, errorResId = null))
        viewModelScope.launch {
            try {
                saveCollection(editor.collectionId, submitted)
                setState(null)
            } catch (cancelled: CancellationException) {
                setState(editor.copy(initialDraft = draft))
                throw cancelled
            } catch (_: Exception) {
                setState(
                    editor.copy(
                        initialDraft = draft,
                        errorResId = if (editor.collectionId == null) R.string.error_collection_create
                        else R.string.error_collection_update,
                    )
                )
            }
        }
    }

    private fun setState(value: CollectionEditorState?) {
        savedStateHandle[STATE_KEY] = value?.let { Json.encodeToString(it) }
        _state.value = value
    }

    class Factory(private val useCases: CollectionUseCases) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return CollectionEditorViewModel(extras.createSavedStateHandle()) { id, draft ->
                try {
                    val cover = draft.coverUri?.let { Image(uri = it.toUri()) }
                    if (id == null) {
                        useCases.createCollection(Collection(name = draft.name, cover = cover))
                    } else {
                        val original = useCases.getAllCollections().first().firstOrNull { it.id == id }
                            ?: error("Collection $id no longer exists")
                        useCases.updateCollection(original.copy(name = draft.name, cover = cover))
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Log.w("CollectionEditor", "Could not save collection", failure)
                    throw failure
                }
            } as T
        }
    }

    private companion object {
        const val STATE_KEY = "collectionEditor"
    }
}
