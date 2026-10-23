package com.farywave.memehive.ui.activity.main.screen.editing

import android.content.ContentResolver
import android.os.Bundle
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farywave.memehive.ui.mapper.toDraft
import com.farywave.memehive.ui.mapper.toMediaItem
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.R
import com.farywave.memehive.ui.model.UiError
import com.farywave.memehive.ui.model.MediaItemDraft
import com.farywave.memehive.ui.use_case.CollectionUseCases
import com.farywave.memehive.ui.use_case.MediaItemUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditingViewModel(
    mediaItemId: Long,
    private val mediaItemUseCases: MediaItemUseCases,
    private val collectionUseCases: CollectionUseCases,
    private val contentResolver: ContentResolver,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val restoredDraft = savedStateHandle.get<Bundle>(DRAFT_KEY)?.toDraft()

    private val _mediaItemDraft = MutableStateFlow(restoredDraft ?: MediaItemDraft(id = mediaItemId))
    val mediaItemDraft = _mediaItemDraft.asStateFlow()

    private var originalMediaItem: MediaItemDraft? = null

    private val _refresh = MutableStateFlow(0)

    private val isEdited = combine(mediaItemDraft, _refresh) { draft, _ ->
        !draft.hasSameContentAs(originalMediaItem)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val screenState = combine(mediaItemDraft, isEdited) { draft, edited ->
        when {
            (draft.id == 0L) && !edited -> ScreenState.BLANK
            draft.id == 0L -> ScreenState.CREATION
            edited -> ScreenState.EDITING
            else -> ScreenState.SAVED
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = if (mediaItemId == 0L) {
            ScreenState.CREATION
        } else {
            ScreenState.SAVED
        }
    )

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<UiError?>(null)
    val error = _error.asStateFlow()

    private val _collections = MutableStateFlow<List<Collection>>(emptyList())
    val collections = _collections.asStateFlow()

    private var nextTagId = restoredDraft?.tags?.size ?: 0

    init {
        savedStateHandle.setSavedStateProvider(DRAFT_KEY) { _mediaItemDraft.value.toSavedBundle() }
        loadCollections()
        val initialDraft = _mediaItemDraft.value
        viewModelScope.launch {
            val id = restoredDraft?.id ?: mediaItemId
            val original = if (id == 0L) MediaItemDraft() else {
                mediaItemUseCases.getMediaItemById(id)?.let { item ->
                    item.toDraft(collectionUseCases.getCollectionIdsByMediaItem(item.id))
                } ?: MediaItemDraft()
            }
            saveOriginalValue(original)
            if (restoredDraft == null && _mediaItemDraft.value == initialDraft) {
                _mediaItemDraft.value = original
                nextTagId = original.tags.size
            }
            val image = _mediaItemDraft.value.image
            if (image != null && !canReadImage(image.uri)) {
                _mediaItemDraft.update { current ->
                    if (current.image == image) current.copy(image = null) else current
                }
            }
        }
    }

    suspend fun loadMediaItem(id: Long) {
        if (id != 0L) {
            val item = mediaItemUseCases.getMediaItemById(id)
            if (item != null) {
                val collectionIds = collectionUseCases.getCollectionIdsByMediaItem(item.id)
                _mediaItemDraft.value = item.toDraft(collectionIds)
                nextTagId = _mediaItemDraft.value.tags.size
            }
        }
    }

    fun loadCollections() {
        viewModelScope.launch {
            collectionUseCases.getAllCollections()
                .collect { list ->
                    _collections.value = list
                }
        }
    }

    fun updateName(name: String) {
        _mediaItemDraft.update { current ->
            current.copy(caption = name)
        }
    }

    fun updateDescription(description: String) {
        _mediaItemDraft.update { current ->
            current.copy(description = description)
        }
    }

    fun updateImage(uri: Uri?) {
        _mediaItemDraft.update { current ->
            current.copy(image = uri?.let { Image(uri = uri) })
        }
    }

    fun addTag(text: String) {
        if (_mediaItemDraft.value.tags.any { it.text == text }) return
        _mediaItemDraft.update { current ->
            current.copy(tags = current.tags + MediaItemDraft.EditableTag(generateTagId(), text))
        }
    }

    fun updateTag(key: Int, newText: String) {
        if (_mediaItemDraft.value.tags.any { it.text == newText && it.key != key }) removeTag(key)
        _mediaItemDraft.update { current ->
            current.copy(tags = current.tags.map {
                if (it.key == key) it.copy(text = newText) else it
            })
        }
    }

    fun removeTag(key: Int) {
        _mediaItemDraft.update { current ->
            current.copy(tags = current.tags.filterNot { it.key == key })
        }
    }

    fun toggleCollection(collection: Collection) {
        if (_mediaItemDraft.value.collectionIds.contains(collection.id))
            _mediaItemDraft.update { it.copy(collectionIds = _mediaItemDraft.value.collectionIds - collection.id) }
        else _mediaItemDraft.update { it.copy(collectionIds = _mediaItemDraft.value.collectionIds + collection.id) }
    }

    fun createCollection(name: String) {
        viewModelScope.launch {
            collectionUseCases.createCollection(
                Collection(name = name)
            )
        }
    }

    fun setCurrentItemAsCover(collection: Collection) {
        viewModelScope.launch {
            originalMediaItem?.image?.let {
                collectionUseCases.setCollectionCover(
                    collection.id,
                    it.id
                )
            }
        }
    }

    fun saveOriginalValue(newValue: MediaItemDraft? = null) {
        originalMediaItem = newValue ?: _mediaItemDraft.value

        _refresh.update { it + 1 }
    }

    fun saveCurrentItem() {
        if (_isSaving.value) return

        _isSaving.value = true
        _error.value = null
        val draft = _mediaItemDraft.value

        viewModelScope.launch {
            try {
                val saved = if (draft.id == 0L) {
                    mediaItemUseCases.createMediaItem(
                        draft.toMediaItem(),
                        draft.collectionIds
                    ).toDraft(draft.collectionIds)
                } else {
                    mediaItemUseCases.updateMediaItem(
                        draft.toMediaItem(),
                        draft.collectionIds
                    ).toDraft(draft.collectionIds)
                }
                saveOriginalValue(saved)
                _mediaItemDraft.update { current ->
                    current.copy(
                        id = saved.id,
                        image = if (current.image == draft.image) saved.image else current.image
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _error.value = UiError(R.string.editing_save_error)
                Log.w("MEDIA_SAVING", "Failed to save media", exception)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun discardChanges() {
        if (_isSaving.value) return
        originalMediaItem?.let { _mediaItemDraft.value = it }

        _refresh.update { it + 1 }
    }

    fun duplicateCurrentItem() {
        if (_isSaving.value) return
        viewModelScope.launch {
            originalMediaItem?.let {
                mediaItemUseCases.createMediaItem(
                    it.toMediaItem().copy(id = 0L)
                )
            }
        }
    }

    fun deleteCurrentItem() {
        if (_isSaving.value) return
        viewModelScope.launch {
            originalMediaItem?.let { mediaItemUseCases.deleteMediaItem(it.toMediaItem()) }
        }
    }

    fun clearError(id: String) {
        _error.update { current -> current?.takeUnless { it.id == id } }
    }

    private fun generateTagId() = nextTagId++

    private suspend fun canReadImage(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri)?.use { it.read() != -1 } ?: false
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }

    private companion object {
        const val DRAFT_KEY = "editorDraft"
    }
}

internal fun MediaItemDraft.toSavedBundle() = Bundle().apply {
    putLong("id", id)
    putString("caption", caption)
    putString("description", description)
    putStringArrayList("tags", ArrayList(tags.map { it.text }))
    putLongArray("collectionIds", collectionIds.toLongArray())
    putString("imageUri", image?.uri?.toString())
    putLong("imageId", image?.id ?: 0L)
}

internal fun Bundle.toDraft() = MediaItemDraft(
    id = getLong("id"),
    caption = getString("caption").orEmpty(),
    description = getString("description").orEmpty(),
    tags = getStringArrayList("tags").orEmpty().mapIndexed { index, text ->
        MediaItemDraft.EditableTag(index, text)
    },
    collectionIds = getLongArray("collectionIds")?.toSet().orEmpty(),
    image = getString("imageUri")?.let { Image(id = getLong("imageId"), uri = Uri.parse(it)) },
)
