package com.farywave.memehive.ui.activity.main.screen.editing

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.Image
import com.farywave.memehive.ui.model.MediaItem
import com.farywave.memehive.ui.use_case.CollectionUseCases
import com.farywave.memehive.ui.use_case.MediaItemUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditingViewModel(
    mediaItemId: Long,
    val mediaItemUseCases: MediaItemUseCases,
    val collectionUseCases: CollectionUseCases,
) : ViewModel() {

    private val _mediaItem = MutableStateFlow(
        MediaItem(
            -1,
            null,
            "",
            "",
            emptyList()
        )
    )
    val mediaItem = _mediaItem.asStateFlow()

    private val _editableTags = MutableStateFlow<List<EditableTag>>(emptyList())
    val editableTags = _editableTags.asStateFlow()

    private val allCollection = Collection(-1, "All")
    private val _collections = MutableStateFlow(listOf(allCollection))
    val collections = _collections.asStateFlow()

    private val _selectedCollections = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCollections = _selectedCollections.asStateFlow()


    private var originalMediaItem: MediaItem? = null
    private var originalTags: List<EditableTag> = emptyList()
    private var originalCollections: Set<Long> = emptySet()
    private val _refresh = MutableStateFlow(0)

    val isEdited =
        combine(
            mediaItem,
            editableTags,
            selectedCollections,
            _refresh

        ) { mediaItem, tags, selectedCollections, _ ->
            originalMediaItem?.image?.uri != mediaItem.image?.uri ||
            originalMediaItem?.caption != mediaItem.caption ||
                    originalMediaItem?.description != mediaItem.description ||
                    originalTags.map { it.text } != tags.map { it.text } ||
                    originalCollections != selectedCollections
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    init {
        loadMediaItem(mediaItemId)
        loadCollections()
    }

    private var nextId = 0L

    private fun generateId(): Long {
        return nextId++
    }

    data class EditableTag(
        val id: Long,
        val text: String
    )

    fun saveOriginalValues() {
        originalMediaItem = mediaItem.value
        originalTags = editableTags.value
        originalCollections = selectedCollections.value

        _refresh.update { it + 1 }
    }

    fun discardChanges() {
        originalMediaItem?.let { _mediaItem.value = it }
        _editableTags.value = originalTags
        _selectedCollections.value = originalCollections

        _refresh.update { it + 1 }
    }

    fun loadMediaItem(id: Long) {
        if (id != -1L) {
            viewModelScope.launch {
                val item = mediaItemUseCases.getMediaItemByID(id)

                if (item != null) {
                    _mediaItem.value = item

                    _editableTags.value = item.tags.map {
                        EditableTag(
                            id = generateId(),
                            text = it
                        )
                    }

                    loadSelectedCollections()
                    saveOriginalValues()
                }
            }
        } else {
            _selectedCollections.value = emptySet()
            saveOriginalValues()
        }
    }

    fun loadCollections() {
        viewModelScope.launch(Dispatchers.IO) {
            collectionUseCases.getAllCollections()
                .collect { list ->
                    _collections.value = listOf(allCollection) + list
                }
        }
    }

    suspend fun loadSelectedCollections() {

        val entries = collectionUseCases
            .getCollectionsByMediaItem(mediaItem.value.id)
            .first()

        _selectedCollections.value = entries
            .map { it.collectionId }
            .toSet()
    }

    fun toggleCollection(collection: Collection) {
        if (_selectedCollections.value.contains(collection.id)) _selectedCollections.value -= collection.id
        else _selectedCollections.value += collection.id
    }

    fun createCollection(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionUseCases.createCollection(
                Collection(name = name)
            )
        }
    }

    fun updateCollectionEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            collectionUseCases.updateMediaItemCollections(
                _mediaItem.value.id,
                _selectedCollections.value.toList()
            )
        }
    }

    fun updateName(name: String) {
        _mediaItem.update { current ->
            current.copy(caption = name)
        }
    }

    fun updateDescription(description: String) {
        _mediaItem.update { current ->
            current.copy(description = description)
        }
    }

    fun updateTag(id: Long, newText: String) {
        if (_editableTags.value.any { it.text == newText && it.id != id }) removeTag(id)
        _editableTags.update { list ->
            list.map {
                if (it.id == id) it.copy(text = newText) else it
            }
        }
    }

    fun removeTag(id: Long) {
        _editableTags.update { list ->
            list.filterNot { it.id == id }
        }
    }

    fun addTag(text: String) {
        if (_editableTags.value.any { it.text == text }) return
        _editableTags.update { list ->
            list + EditableTag(generateId(), text)
        }
    }

    private fun commitTags() {
        val cleanTags = _editableTags.value
            .map { it.text.trim() }
            .filter { it.isNotEmpty() }

        _mediaItem.update {
            it.copy(tags = cleanTags)
        }
    }

    fun updateImage(uri: Uri?) {
        _mediaItem.value = _mediaItem.value.copy(image = uri?.let { Image(uri = it) })
    }

    fun onSave() {
        if (_mediaItem.value.id == -1L && _mediaItem.value.image == null || !isEdited.value) return
        commitTags()
        viewModelScope.launch(Dispatchers.IO) {
            if (_mediaItem.value.id == -1L) {
                mediaItemUseCases.createMediaItem(_mediaItem.value)
            } else {
                mediaItemUseCases.updateMediaItem(_mediaItem.value)
            }
        }
        updateCollectionEntries()
    }

    fun onDuplicate() {
        viewModelScope.launch(Dispatchers.IO) {
            mediaItemUseCases.createMediaItem(mediaItem.value.copy(id = 0L))
        }
    }

    fun onDelete() {
        viewModelScope.launch(Dispatchers.IO) {
            mediaItemUseCases.deleteMediaItem(_mediaItem.value)
        }
    }

    fun onSetAsCover(collection: Collection) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionUseCases.setCollectionCover(collection.id, _mediaItem.value.id)
        }
    }
}

