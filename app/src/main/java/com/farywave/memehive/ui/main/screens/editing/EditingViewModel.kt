package com.farywave.memehive.ui.main.screens.editing

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.farywave.memehive.core.FileManager
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.ui.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class EditingViewModel(
    mediaItemId: Long,
    mediaItemRepository: MediaItemRepository,
    collectionRepository: CollectionRepository
) : ViewModel() {

    private val _mediaItem = MutableStateFlow(
        if (mediaItemId == -1L) MediaItem(
            -1,
            null,
            null,
            null,
            emptyList()
        ) else MediaItem(mediaItemId, null, null, null, emptyList())
    )
    val mediaItem = _mediaItem.asStateFlow()

    private val _editableTags = MutableStateFlow<List<EditableTag>>(emptyList())
    val editableTags = _editableTags.asStateFlow()

    private val _mediaSrc = MutableStateFlow(_mediaItem.value.src?.toUri())
    val mediaSrc = _mediaSrc.asStateFlow()

    init {
        _editableTags.value = _mediaItem.value.tags.map {
            EditableTag(
                id = generateId(),
                text = it
            )
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
        _mediaItem.value.src?.let { FileManager.deleteFromInternalStorage(it) }
        commitTags()
        updateMediaItemSrc(_mediaSrc.value?.let { FileManager.copyToInternalStorage(context, it) })
    }
}