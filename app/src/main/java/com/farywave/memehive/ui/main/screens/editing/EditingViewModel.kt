package com.farywave.memehive.ui.main.screens.editing

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farywave.memehive.core.FileManager
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.ui.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class EditingViewModel(
    val mediaItemId: Long,
    val mediaItemRepository: MediaItemRepository,
    val collectionRepository: CollectionRepository
) : ViewModel() {

    private val _mediaItem = MutableStateFlow(
        MediaItem(
            -1,
            null,
            null,
            null,
            emptyList()
        )
    )
    val mediaItem = _mediaItem.asStateFlow()

    private val _editableTags = MutableStateFlow<List<EditableTag>>(emptyList())
    val editableTags = _editableTags.asStateFlow()

    private val _mediaSrc = MutableStateFlow(_mediaItem.value.src?.toUri())
    val mediaSrc = _mediaSrc.asStateFlow()

    init {
        if (mediaItemId != -1L) {
            viewModelScope.launch {
                val item = mediaItemRepository.getMediaItemById(mediaItemId)
                Log.d("EditingViewModel", "init: ${item?.caption}")

                if (item != null) {
                    _mediaItem.value = item

                    _editableTags.value = item.tags.map {
                        EditableTag(
                            id = generateId(),
                            text = it
                        )
                    }

                    _mediaSrc.value = item.src?.toUri()
                }
            }
        }
    }

    private var nextId = 0L

    private fun generateId(): Long {
        return nextId++
    }

    data class EditableTag(
        val id: Long,
        val text: String
    )

    fun updateMediaItemSrc(src: File?) {
        _mediaItem.update { current ->
            current.copy(src = src)
        }
    }

    fun updateName(name: String?) {
        _mediaItem.update { current ->
            current.copy(caption = name)
        }
    }

    fun updateDescription(description: String?) {
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

    fun commitTags() {
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
        commitTags()

        val oldSrc = _mediaItem.value.src
        val newSrc = _mediaSrc.value

        if (newSrc != null && newSrc != oldSrc) {
            val newInternal = FileManager.copyToInternalStorage(context, newSrc)
            _mediaItem.value.src = newInternal

            oldSrc?.let { FileManager.deleteFromInternalStorage(it) }
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (mediaItemId == -1L) mediaItemRepository.insertMediaItem(_mediaItem.value)
            else mediaItemRepository.updateMediaItem(_mediaItem.value)
        }
    }
}

