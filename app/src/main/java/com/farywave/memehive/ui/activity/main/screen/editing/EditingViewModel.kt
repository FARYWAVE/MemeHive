package com.farywave.memehive.ui.activity.main.screen.editing

import android.net.Uri
import androidx.core.net.toUri
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farywave.memehive.R
import com.farywave.memehive.application.usecase.CollectionUseCases
import com.farywave.memehive.application.usecase.MediaItemUseCases
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.ui.mapper.toDraft
import com.farywave.memehive.ui.mapper.toMediaItem
import com.farywave.memehive.ui.model.MediaItemDraft
import com.farywave.memehive.ui.model.UiMessageQueue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditingViewModel(
    private val mediaItemId: Long,
    private val mediaItemUseCases: MediaItemUseCases,
    private val collectionUseCases: CollectionUseCases,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val restoredDraft = savedStateHandle.get<Bundle>(DRAFT_KEY)?.toDraft()

    private val _mediaItemDraft =
        MutableStateFlow(restoredDraft ?: MediaItemDraft(id = mediaItemId))
    val mediaItemDraft = _mediaItemDraft.asStateFlow()

    private var savedDraft: MediaItemDraft? = null

    private val _refresh = MutableStateFlow(0)

    private val hasUnsavedChanges =
        combine(mediaItemDraft, _refresh) { draft, _ ->
                !draft.hasSameContentAs(savedDraft)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                false,
            )

    val contentState =
        combine(mediaItemDraft, hasUnsavedChanges) { draft, edited ->
                when {
                    (draft.id == 0L) && !edited -> EditorContentState.BLANK
                    draft.id == 0L -> EditorContentState.CREATION
                    edited -> EditorContentState.EDITING
                    else -> EditorContentState.SAVED
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue =
                    if (mediaItemId == 0L) {
                        EditorContentState.CREATION
                    } else {
                        EditorContentState.SAVED
                    },
            )

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted = _isDeleted.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val messages = UiMessageQueue()
    val error = messages.current

    private val _collections = MutableStateFlow<List<Collection>>(emptyList())
    val collections = _collections.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()
    private val _loadError = MutableStateFlow<Int?>(null)
    val loadError = _loadError.asStateFlow()
    private val _hasCollectionsLoadError = MutableStateFlow(false)
    val hasCollectionsLoadError = _hasCollectionsLoadError.asStateFlow()
    private val _closeRequested = MutableStateFlow(false)
    val closeRequested = _closeRequested.asStateFlow()
    private var collectionsJob: Job? = null
    private var loadJob: Job? = null

    private var nextTagKey = restoredDraft?.tags?.size ?: 0

    init {
        savedStateHandle.setSavedStateProvider(DRAFT_KEY) { _mediaItemDraft.value.toSavedBundle() }
        loadCollections()
        retryLoading()
    }

    fun retryLoading() {
        if (loadJob?.isActive == true) return
        _isLoading.value = true
        _loadError.value = null
        val initialDraft = _mediaItemDraft.value
        loadJob = viewModelScope.launch {
            try {
                val id = restoredDraft?.id ?: mediaItemId
                val original =
                    if (id == 0L) MediaItemDraft()
                    else {
                        val item = mediaItemUseCases.getMediaItemById(id)
                        if (item == null) {
                            _loadError.value = R.string.error_media_missing
                            return@launch
                        }
                        item.toDraft(collectionUseCases.getCollectionIdsByMediaItem(item.id))
                    }
                updateSavedDraft(original)
                if (restoredDraft == null && _mediaItemDraft.value == initialDraft) {
                    _mediaItemDraft.value = original
                    nextTagKey = original.tags.size
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Log.w("EditingViewModel", "Could not load media", failure)
                _loadError.value = R.string.error_media_load
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCollections() {
        collectionsJob?.cancel()
        _hasCollectionsLoadError.value = false
        collectionsJob = viewModelScope.launch {
            collectionUseCases
                .observeCollections()
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    Log.w("EditingViewModel", "Could not load collections", failure)
                    _hasCollectionsLoadError.value = true
                }
                .collect { list ->
                    _hasCollectionsLoadError.value = false
                    _collections.value = list
                }
        }
    }

    fun updateCaption(caption: String) {
        if (_isSaving.value) return
        _mediaItemDraft.update { current ->
            current.copy(caption = caption)
        }
    }

    fun updateDescription(description: String) {
        if (_isSaving.value) return
        _mediaItemDraft.update { current ->
            current.copy(description = description)
        }
    }

    fun updateImage(uri: Uri?) {
        if (_isSaving.value) return
        _mediaItemDraft.update { current ->
            current.copy(image = uri?.let { Image(uri = uri) })
        }
    }

    fun addTag(text: String) {
        if (_isSaving.value) return
        if (_mediaItemDraft.value.tags.any { it.text == text }) return
        _mediaItemDraft.update { current ->
            current.copy(tags = current.tags + MediaItemDraft.EditableTag(generateTagKey(), text))
        }
    }

    fun updateTag(key: Int, newText: String) {
        if (_isSaving.value) return
        if (_mediaItemDraft.value.tags.any { it.text == newText && it.key != key }) removeTag(key)
        _mediaItemDraft.update { current ->
            current.copy(
                tags =
                    current.tags.map {
                        if (it.key == key) it.copy(text = newText) else it
                    }
            )
        }
    }

    fun removeTag(key: Int) {
        if (_isSaving.value) return
        _mediaItemDraft.update { current ->
            current.copy(tags = current.tags.filterNot { it.key == key })
        }
    }

    fun toggleCollection(collection: Collection) {
        if (_isSaving.value) return
        if (_mediaItemDraft.value.collectionIds.contains(collection.id))
            _mediaItemDraft.update {
                it.copy(collectionIds = _mediaItemDraft.value.collectionIds - collection.id)
            }
        else
            _mediaItemDraft.update {
                it.copy(collectionIds = _mediaItemDraft.value.collectionIds + collection.id)
            }
    }

    fun setCurrentItemAsCover(collection: Collection) {
        val image = savedDraft?.image
        if (image == null) {
            messages.show(R.string.error_image_required)
            return
        }
        launchMutation(R.string.error_collection_cover, R.string.success_collection_cover) {
            collectionUseCases.setCollectionCover(collection.id, image.id)
        }
    }

    fun updateSavedDraft(draft: MediaItemDraft? = null) {
        savedDraft = draft ?: _mediaItemDraft.value

        _refresh.update { it + 1 }
    }

    fun saveCurrentItem(closeAfterSave: Boolean = false) {
        if (_isSaving.value || _isLoading.value || _loadError.value != null) return

        _isSaving.value = true
        val draft = _mediaItemDraft.value

        viewModelScope.launch {
            try {
                val saved =
                    if (draft.id == 0L) {
                        mediaItemUseCases
                            .createMediaItem(
                                draft.toMediaItem(),
                                draft.collectionIds,
                            )
                            .toDraft(draft.collectionIds)
                    } else {
                        mediaItemUseCases
                            .updateMediaItem(
                                draft.toMediaItem(),
                                draft.collectionIds,
                            )
                            .toDraft(draft.collectionIds)
                    }
                updateSavedDraft(saved)
                _mediaItemDraft.update { current ->
                    current.copy(
                        id = saved.id,
                        image = if (current.image == draft.image) saved.image else current.image,
                    )
                }
                if (closeAfterSave) _closeRequested.value = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                messages.show(R.string.error_media_save)
                Log.w("MEDIA_SAVING", "Failed to save media", exception)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun discardChanges() {
        if (_isSaving.value) return
        savedDraft?.let { _mediaItemDraft.value = it }

        _refresh.update { it + 1 }
    }

    fun duplicateCurrentItem() {
        val item = savedDraft ?: return
        launchMutation(R.string.error_media_duplicate, R.string.success_media_duplicated) {
            mediaItemUseCases.createMediaItem(item.toMediaItem().copy(id = 0L))
        }
    }

    private fun launchMutation(error: Int, success: Int, operation: suspend () -> Unit) {
        if (_isSaving.value || _isLoading.value || _loadError.value != null) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                operation()
                messages.show(success)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Log.w("EditingViewModel", "Could not update media", failure)
                messages.show(error)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteCurrentItem() {
        if (_isSaving.value || _isDeleted.value) return
        val item = savedDraft ?: return
        if (item.id == 0L) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                mediaItemUseCases.deleteMediaItem(item.toMediaItem())
                _isDeleted.value = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                messages.show(R.string.error_media_delete)
                Log.w("MEDIA_DELETING", "Failed to delete media", failure)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearError(id: String) {
        messages.dismiss(id)
    }

    private fun generateTagKey() = nextTagKey++

    private companion object {
        const val DRAFT_KEY = "editorDraft"
    }
}

internal fun MediaItemDraft.toSavedBundle() =
    Bundle().apply {
        putLong("id", id)
        putString("caption", caption)
        putString("description", description)
        putStringArrayList("tags", ArrayList(tags.map { it.text }))
        putLongArray("collectionIds", collectionIds.toLongArray())
        putString("imageUri", image?.uri?.toString())
        putLong("imageId", image?.id ?: 0L)
    }

internal fun Bundle.toDraft() =
    MediaItemDraft(
        id = getLong("id"),
        caption = getString("caption").orEmpty(),
        description = getString("description").orEmpty(),
        tags =
            getStringArrayList("tags").orEmpty().mapIndexed { index, text ->
                MediaItemDraft.EditableTag(index, text)
            },
        collectionIds = getLongArray("collectionIds")?.toSet().orEmpty(),
        image = getString("imageUri")?.let { Image(id = getLong("imageId"), uri = it.toUri()) },
    )
