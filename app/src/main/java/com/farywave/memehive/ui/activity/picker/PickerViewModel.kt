package com.farywave.memehive.ui.activity.picker

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.MediaItem
import com.farywave.memehive.ui.use_case.CollectionUseCases
import com.farywave.memehive.ui.use_case.MediaItemUseCases
import com.farywave.memehive.ui.use_case.media_item_search.MediaSearchUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PickerViewModel(
    val collectionUseCases: CollectionUseCases,
    val mediaSearchUseCase: MediaSearchUseCase
) : ViewModel() {
    private val allCollection =
        Collection(-1, "All", cover = null, mediaItemCount = 0)
    private val _collections = MutableStateFlow(listOf(allCollection))
    val collections = _collections.asStateFlow()

    private val _selectedCollection = MutableStateFlow(_collections.value.first())
    val selectedCollection = _selectedCollection.asStateFlow()

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems = _mediaItems.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    init {
        onRefresh()
        loadCollections()
    }

    fun loadCollections() {
        viewModelScope.launch(Dispatchers.IO) {
            collectionUseCases.getAllCollections()
                .collect { list ->
                    _collections.value = listOf(allCollection) + list
                }
        }
    }

    fun onSearch() {
        viewModelScope.launch(Dispatchers.IO) {
            _mediaItems.value =
                mediaSearchUseCase.invoke(selectedCollection.value.id, _searchQuery.value)
                    .filter { it.image != null }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onRefresh() {
        onSearch()
    }

    fun onCollectionSelected(collection: Collection) {
        _selectedCollection.value = collection
    }
}