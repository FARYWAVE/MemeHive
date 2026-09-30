package com.farywave.memehive.ui.activity.main.screen.hive


import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farywave.memehive.R
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.core.collection_transfer.CollectionTransferTool
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.ImageRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.Image
import com.farywave.memehive.ui.model.MediaItem
import com.farywave.memehive.ui.use_case.CollectionTransferUseCases
import com.farywave.memehive.ui.use_case.CollectionUseCases
import com.farywave.memehive.ui.use_case.MediaItemUseCases
import com.farywave.memehive.ui.use_case.media_item_search.MediaSearchUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HiveViewModel(
    val mediaItemUseCases: MediaItemUseCases,
    val collectionUseCases: CollectionUseCases,
    val mediaSearchUseCase: MediaSearchUseCase,
    val collectionTransferUseCases: CollectionTransferUseCases
) : ViewModel() {
    private val allCollection = Collection(-1, "All", cover = null, mediaItemCount = 0)
    private val _collections = MutableStateFlow(listOf(allCollection))
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
                mediaSearchUseCase.invoke(selectedCollection.value.id, searchQuery.value)

            disableMassEditingMode()
        }
    }

    fun onRefresh() {
        onSearch()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCollectionSelected(collection: Collection) {
        _selectedCollection.value = collection
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
        val selected = _mediaItems.value.filter { it.isSelected }

        viewModelScope.launch(Dispatchers.IO) {
            mediaItemUseCases.deleteMediaItemsByIDs(selected.map { it.id })
            onRefresh()
        }
    }

    fun duplicateSelectedMediaItems() {
        val selected = _mediaItems.value.filter { it.isSelected }

        viewModelScope.launch(Dispatchers.IO) {
            selected.forEach {
                mediaItemUseCases.createMediaItem(it.copy(id = 0))
            }
            onRefresh()
        }
    }

    fun addSelectedMediaItemsToCollection(collection: Collection) {
        val selected = _mediaItems.value.filter { it.isSelected }

        viewModelScope.launch(Dispatchers.IO) {
            collectionUseCases.addItemsToCollection(selected.map { it.id }.toSet(), collection.id)
            onRefresh()
        }
    }

    fun onMassImport(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val mediaItemIDs = uris.map {
                mediaItemUseCases.createMediaItem(
                    MediaItem(
                        image = Image(uri = it),
                    )
                )
            }

            collectionUseCases.addItemsToCollection(
                mediaItemIDs.toSet(),
                _selectedCollection.value.id
            )

            onRefresh()
        }
    }

    fun updateCollection(collectionId: Long, newName: String?, newCover: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            val collection = collections.value.find { it.id == collectionId } ?: return@launch

            collectionUseCases.updateCollection(
                collection.copy(
                    name = newName ?: collection.name,
                    cover = newCover?.let { Image(uri = it) } ?: collection.cover
                )
            )
        }
    }

    fun deleteCollection(collection: Collection) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionUseCases.deleteCollection(collection)

            if (collection.id == _selectedCollection.value.id) {
                _selectedCollection.value = allCollection
                onRefresh()
            }
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