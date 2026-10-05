package com.farywave.memehive.ui.activity.main.screen.hive

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.Image
import com.farywave.memehive.ui.model.MediaItem
import com.farywave.memehive.ui.use_case.CollectionTransferUseCases
import com.farywave.memehive.ui.use_case.CollectionUseCases
import com.farywave.memehive.ui.use_case.MediaItemUseCases
import com.farywave.memehive.ui.use_case.media_item_search.MediaSearchUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HiveViewModel(
    val mediaItemUseCases: MediaItemUseCases,
    val collectionUseCases: CollectionUseCases,
    val mediaSearchUseCase: MediaSearchUseCase,
    val collectionTransferUseCases: CollectionTransferUseCases
) : ViewModel() {
    private val _collections = MutableStateFlow<List<Collection>>(emptyList())
    val collections = _collections.asStateFlow()

    private val _selectedCollectionId = MutableStateFlow<Long?>(null)
    val selectedCollectionId = _selectedCollectionId.asStateFlow()

    private val _selectedMediaItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedMediaItemIds = _selectedMediaItemIds.asStateFlow()

    private val _isMassEditingMode = MutableStateFlow(false)
    val isMassEditingMode = _isMassEditingMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val mediaItems = mediaSearchUseCase(selectedCollectionId, searchQuery)
        .onEach { items ->
            val visibleIds = items.mapTo(mutableSetOf()) { it.id }
            _selectedMediaItemIds.update { it.intersect(visibleIds) }
            if (_selectedMediaItemIds.value.isEmpty()) _isMassEditingMode.value = false
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadCollections()
    }

    private fun loadCollections() {
        viewModelScope.launch {
            collectionUseCases.getAllCollections().collect { collections ->
                _collections.value = collections
                val selectedId = _selectedCollectionId.value
                if (selectedId != null && collections.none { it.id == selectedId }) {
                    onCollectionSelected(null)
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        if (_searchQuery.value == query) return
        disableMassEditingMode()
        _searchQuery.value = query
    }

    fun onCollectionSelected(collectionId: Long?) {
        if (_selectedCollectionId.value == collectionId) return
        disableMassEditingMode()
        _selectedCollectionId.value = collectionId
    }

    fun disableMassEditingMode() {
        _isMassEditingMode.value = false
        _selectedMediaItemIds.value = emptySet()
    }

    fun enableMassEditingMode() {
        _isMassEditingMode.value = true
    }

    fun toggleItemSelection(itemId: Long) {
        if (itemId in _selectedMediaItemIds.value) _selectedMediaItemIds.value -= itemId
        else _selectedMediaItemIds.value += itemId
        if (_selectedMediaItemIds.value.isEmpty()) disableMassEditingMode()
    }

    fun selectItem(itemId: Long) {
        _selectedMediaItemIds.value += itemId
    }

    fun createCollection(name: String, cover: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionUseCases.createCollection(
                Collection(
                    id = 0,
                    name = name,
                    cover = cover?.let { Image(uri = it) },
                    mediaItemCount = 0
                )
            )
        }
    }

    fun deleteSelectedMediaItems() {
        val selectedIds = _selectedMediaItemIds.value
        viewModelScope.launch(Dispatchers.IO) {
            mediaItemUseCases.deleteMediaItemsByIds(selectedIds)
        }
    }

    fun duplicateSelectedMediaItems() {
        val selected = mediaItems.value.filter { it.id in _selectedMediaItemIds.value }

        viewModelScope.launch(Dispatchers.IO) {
            selected.forEach {
                mediaItemUseCases.createMediaItem(it.copy(id = 0))
            }
        }
    }

    fun addSelectedMediaItemsToCollection(collection: Collection) {
        val selectedIds = _selectedMediaItemIds.value
        viewModelScope.launch(Dispatchers.IO) {
            collectionUseCases.addItemsToCollection(selectedIds, collection.id)
        }
    }

    fun onMassImport(uris: List<Uri>) {
        val collectionId = _selectedCollectionId.value
        viewModelScope.launch(Dispatchers.IO) {
            val mediaItems = uris.map {
                mediaItemUseCases.createMediaItem(
                    MediaItem(
                        image = Image(uri = it),
                    )
                )
            }

            collectionId?.let {
                collectionUseCases.addItemsToCollection(
                    mediaItems.map { item -> item.id }.toSet(),
                    it
                )
            }
        }
    }

    fun updateCollection(collectionId: Long, newName: String?, newCover: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            val collection = collections.value.find { it.id == collectionId } ?: return@launch

            collectionUseCases.updateCollection(
                collection.copy(
                    name = newName ?: collection.name,
                    cover = newCover?.let { Image(uri = it) }
                )
            )
        }
    }

    fun deleteCollection(collection: Collection) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionUseCases.deleteCollection(collection)
        }
    }

    fun exportCollection(collection: Collection, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                collectionTransferUseCases.exportCollection(collection, uri) //TODO Add toasts
            }
        }
    }

    fun importCollection(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                collectionTransferUseCases.importCollection(uri) //TODO Add toasts
            }
        }
    }
}
