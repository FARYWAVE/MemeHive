package com.farywave.memehive.ui.main.screens.editing

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class EditingViewModel(
    mediaItemId: Long,
    val mediaItemRepository: MediaItemRepository,
    val collectionRepository: CollectionRepository
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

    private val _mediaSrc = MutableStateFlow(_mediaItem.value.src?.toUri())
    val mediaSrc = _mediaSrc.asStateFlow()

    private val allCollection = Collection(-1, "All", mediaItemCount = 0)
    private val _collections = MutableStateFlow(listOf(allCollection))
    val collections = _collections.asStateFlow()

    private val _selectedCollections = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCollections = _selectedCollections.asStateFlow()


    private var originalMediaItem: MediaItem? = null
    private var originalTags: List<EditableTag> = emptyList()
    private var originalMediaSrc: Uri? = null
    private var originalCollections: Set<Long> = emptySet()
    private val _refresh = MutableStateFlow(0)

    val isEdited =
        combine(
            mediaItem,
            editableTags,
            mediaSrc,
            selectedCollections,
            _refresh

        ) { mediaItem, tags, src, selectedCollections, _ ->

            originalMediaItem?.caption != mediaItem.caption ||
                    originalMediaItem?.description != mediaItem.description ||
                    originalTags.map { it.text } != tags.map { it.text } ||
                    originalMediaSrc != src || originalCollections != selectedCollections
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
        originalMediaSrc = mediaSrc.value
        originalCollections = selectedCollections.value

        _refresh.update { it + 1 }
    }

    fun discardChanges() {
        originalMediaItem?.let { _mediaItem.value = it }
        _editableTags.value = originalTags
        _mediaSrc.value = originalMediaSrc
        _selectedCollections.value = originalCollections

        _refresh.update { it + 1 }
    }

    fun loadMediaItem(id: Long) {
        if (id != -1L) {
            viewModelScope.launch {
                val item = mediaItemRepository.getMediaItemById(id)

                if (item != null) {
                    _mediaItem.value = item

                    _editableTags.value = item.tags.map {
                        EditableTag(
                            id = generateId(),
                            text = it
                        )
                    }

                    _mediaSrc.value = item.src?.toUri()

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
            collectionRepository.observeCollections()
                .collect { list ->
                    _collections.value = listOf(allCollection) + list
                }
        }
    }

    suspend fun loadSelectedCollections() {

        val entries = collectionRepository
            .getCollectionsByMediaItem(mediaItem.value)
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
            collectionRepository.insertCollection(
                Collection(
                    id = 0,
                    name = name,
                    mediaItemCount = 0
                )
            )
        }
    }

    fun updateCollectionEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            collectionRepository.updateCollectionEntries(_mediaItem.value, _selectedCollections.value)
        }
    }

    fun updateMediaItemSrc(src: File?) {
        _mediaItem.update { current ->
            current.copy(src = src)
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

    fun updateMediaSrc(uri: Uri?) {
        _mediaSrc.value = uri
    }

    fun onSave(context: Context) {
        if (_mediaItem.value.id == -1L && _mediaSrc.value == null || !isEdited.value) return
        commitTags()

        val oldSrc = _mediaItem.value.src
        val newSrc = _mediaSrc.value

        if (newSrc != null && newSrc != oldSrc) {
            val newInternal = DeviceTools.copyToInternalStorage(context, newSrc)
            _mediaItem.value.src = newInternal

            oldSrc?.let { DeviceTools.deleteFromInternalStorage(it) }
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (_mediaItem.value.id == -1L) mediaItemRepository.insertMediaItem(_mediaItem.value)
            else mediaItemRepository.updateMediaItem(_mediaItem.value)

            updateCollectionEntries()
        }
    }

    fun onDuplicate(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val src = _mediaSrc.value?.let { DeviceTools.copyToInternalStorage(context, it) }
            val newItem = _mediaItem.value.copy(
                id = 0,
                src = src,
                tags = (_editableTags.value.map { it.text.trim() }.filter { it.isNotEmpty() })
            )
            mediaItemRepository.insertMediaItem(newItem)
        }
    }

    fun onDelete() {
        viewModelScope.launch(Dispatchers.IO) {
            _mediaItem.value.src?.let { DeviceTools.deleteFromInternalStorage(it) }
            mediaItemRepository.deleteMediaItem(_mediaItem.value)
        }
    }

    fun onSetAsCover(context: Context, collection: Collection) {
        collection.cover?.let { DeviceTools.deleteFromInternalStorage(it) }
        viewModelScope.launch(Dispatchers.IO) {
            if (_mediaSrc.value == null) collectionRepository.updateCollection(collection.copy(cover = null))
            else {
                val cover = DeviceTools.copyToInternalStorage(context, _mediaSrc.value!!)
                collectionRepository.updateCollection(collection.copy(cover = cover))
            }
        }
    }
}

