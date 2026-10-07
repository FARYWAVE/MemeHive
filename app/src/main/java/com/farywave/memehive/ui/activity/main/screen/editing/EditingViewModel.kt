package com.farywave.memehive.ui.activity.main.screen.editing

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farywave.memehive.ui.mapper.toDraft
import com.farywave.memehive.ui.mapper.toMediaItem
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.ui.model.MediaItemDraft
import com.farywave.memehive.ui.use_case.CollectionUseCases
import com.farywave.memehive.ui.use_case.MediaItemUseCases
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
) : ViewModel() {
    private val _mediaItemDraft = MutableStateFlow(
        MediaItemDraft(
            0L,
            null,
            "",
            "",
            emptyList(),
            emptySet()
        )
    )
    val mediaItemDraft = _mediaItemDraft.asStateFlow()

    private var originalMediaItem: MediaItemDraft? = null

    private val _refresh = MutableStateFlow(0)
    private val isEdited = combine(mediaItemDraft, _refresh) { draft, _ ->
        !(draft.image?.uri == originalMediaItem?.image?.uri &&
                draft.caption == originalMediaItem?.caption &&
                draft.description == originalMediaItem?.description &&
                draft.tags.map { it.text }.toSet() == originalMediaItem?.tags?.map { it.text }
            ?.toSet() &&
                draft.collectionIds == originalMediaItem?.collectionIds)
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

    private val _collections = MutableStateFlow<List<Collection>>(emptyList())
    val collections = _collections.asStateFlow()

    private var nextTagId = 0
    private fun generateTagId() = nextTagId++

    init {
        loadCollections()
        viewModelScope.launch {
            loadMediaItem(mediaItemId)
            saveOriginalValue()
        }

    }

    fun saveOriginalValue() {
        originalMediaItem = _mediaItemDraft.value

        _refresh.update { it + 1 }
    }

    fun discardChanges() {
        originalMediaItem?.let { _mediaItemDraft.value = it }

        _refresh.update { it + 1 }
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

    fun addTag(text: String) {
        if (_mediaItemDraft.value.tags.any { it.text == text }) return
        _mediaItemDraft.update { current ->
            current.copy(tags = current.tags + MediaItemDraft.EditableTag(generateTagId(), text))
        }
    }

    fun updateImage(uri: Uri?) {
        _mediaItemDraft.update { current ->
            current.copy(image = uri?.let { Image(uri = uri) })
        }
    }

    fun saveCurrentItem() {
        viewModelScope.launch {
            if (screenState.value == ScreenState.CREATION) {
                _mediaItemDraft.value = mediaItemUseCases.createMediaItem(
                    _mediaItemDraft.value.toMediaItem(),
                    _mediaItemDraft.value.collectionIds
                ).toDraft(_mediaItemDraft.value.collectionIds)
            } else {
                _mediaItemDraft.value = mediaItemUseCases.updateMediaItem(
                    _mediaItemDraft.value.toMediaItem(),
                    _mediaItemDraft.value.collectionIds
                ).toDraft(_mediaItemDraft.value.collectionIds)
            }
            saveOriginalValue()
        }
    }

    fun duplicateCurrentItem() {
        viewModelScope.launch {
            originalMediaItem?.let {
                mediaItemUseCases.createMediaItem(
                    it.toMediaItem().copy(id = 0L)
                )
            }
        }
    }

    fun deleteCurrentItem() {
        viewModelScope.launch {
            originalMediaItem?.let { mediaItemUseCases.deleteMediaItem(it.toMediaItem()) }
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
}
