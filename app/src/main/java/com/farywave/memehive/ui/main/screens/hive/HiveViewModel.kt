package com.farywave.memehive.ui.main.screens.hive


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HiveViewModel(
    val mediaItemRepository: MediaItemRepository,
    val collectionRepository: CollectionRepository
) : ViewModel() {
    private val _collections = MutableStateFlow(listOf(Collection(-1, "All", -1)))
    val collections = _collections.asStateFlow()

    private val _selectedCollection = MutableStateFlow(_collections.value.first())
    val selectedCollection = _selectedCollection.asStateFlow()

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems = _mediaItems.asStateFlow()

    private val _isMassEditingMode = MutableStateFlow(false)
    val isMassEditingMode = _isMassEditingMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    init {
        onSearch()
    }


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

    fun onSearch() {
        viewModelScope.launch(Dispatchers.IO) {
            _mediaItems.value =
                mediaItemRepository.search(selectedCollection.value.id, _searchQuery.value)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
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
                } else it
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