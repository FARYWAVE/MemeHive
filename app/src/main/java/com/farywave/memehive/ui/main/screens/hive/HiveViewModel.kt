package com.farywave.memehive.ui.main.screens.hive


import android.util.Log
import androidx.lifecycle.ViewModel
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HiveViewModel : ViewModel() {
    private val _collections = MutableStateFlow(listOf(Collection(-1, "All", -1)))
    val collections = _collections.asStateFlow()

    private val _selectedCollection = MutableStateFlow(_collections.value.first())
    val selectedCollection = _selectedCollection.asStateFlow()

    private val _mediaItems = MutableStateFlow(emptyList<MediaItem>())
    val mediaItems = _mediaItems.asStateFlow()

    private val _isMassEditingMode = MutableStateFlow(false)
    val isMassEditingMode = _isMassEditingMode.asStateFlow()


    fun loadCollections() {
        _collections.value = listOf(
            Collection(-1, "All", -1),
            Collection(1, "John Pork", -1),
            Collection(2, "IShowSpeed", -1),
            Collection(3, "Games", -1),
            Collection(4, "Ambatukam", -1),
        )
    }

    fun loadMediaItems() {
        _mediaItems.value =
            (0..100L).map {
                MediaItem(
                    it,
                    null,
                    "Name $it",
                    "Description $it",
                    (0..it).map { it2 -> "Tag $it2" }
                )
            }
    }

    fun onSearch(query: String) {
        Log.d("HiveViewModel", "onSearch: $query")
    }

    fun onCollectionSelected(collection: Collection) {
        _selectedCollection.value = collection
        Log.d("HiveViewModel", "onCollectionSelected: $collection")
    }

    fun onMediaItemOpened(mediaItem: MediaItem) {
        Log.d("HiveViewModel", "onMediaItemOpened: ${mediaItem.caption}")
    }

    fun disableMassEditingMode() {
        _isMassEditingMode.value = false
        _mediaItems.update { list ->
            list.map { it.copy(isSelected = false) }
        }
    }

    fun enableMassEditingMode() {
        _isMassEditingMode.value = true
    }

    fun toggleItemSelection(item: MediaItem) {
        _mediaItems.update { list ->
            list.map {
                if (it.id == item.id) {
                    it.copy(isSelected = !it.isSelected)
                }
                else it
            }
        }
        if (!_mediaItems.value.any { it.isSelected }) disableMassEditingMode()
    }

    fun selectItem(item: MediaItem) {
        _mediaItems.update { list ->
            list.map {
                if (it.id == item.id) it.copy(isSelected = true)
                else it
            }
        }
    }

    fun getSelectedMediaItems() = _mediaItems.value.filter { it.isSelected }
}