package com.farywave.memehive.ui.main.screens.editing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.farywave.memehive.ui.model.MediaItem
import com.farywave.memehive.ui.model.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class EditingViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val mediaItemId: Long =
        checkNotNull(savedStateHandle["mediaItemId"])
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

    fun updateSrc(src: File?) {
        _mediaItem.update { current ->
            current.copy(src = src)
        }
    }

    fun updateName(name: String?) {
        _mediaItem.update { current ->
            current.copy(name = name)
        }
    }

    fun updateDescription(description: String?) {
        _mediaItem.update { current ->
            current.copy(description = description)
        }
    }

    fun updateTags(tags: List<Tag>) {
        _mediaItem.update { current ->
            current.copy(tags = tags)
        }
    }
}