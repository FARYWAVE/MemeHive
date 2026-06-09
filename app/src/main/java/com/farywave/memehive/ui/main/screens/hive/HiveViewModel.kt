package com.farywave.memehive.ui.main.screens.hive


import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farywave.memehive.R
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.core.collection_transfer.CollectionTransferTool
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HiveViewModel(
    val mediaItemRepository: MediaItemRepository,
    val collectionRepository: CollectionRepository,
    private val initialPremium: Boolean
) : ViewModel() {
    private val allCollection = Collection(-1, "All", mediaItemCount = 0)
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

    private val _isPremium = MutableStateFlow(initialPremium)
    val isPremium = _isPremium.asStateFlow()



    init {
        onRefresh()
        loadCollections()
    }

    fun loadCollections() {
        viewModelScope.launch(Dispatchers.IO) {
            collectionRepository.observeCollections()
                .collect { list ->
                    _collections.value = listOf(allCollection) + list
                }
        }
    }

    fun onSearch() {
        viewModelScope.launch(Dispatchers.IO) {
            _mediaItems.value =
                mediaItemRepository.search(selectedCollection.value.id, _searchQuery.value)

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


    fun createCollection(name: String, cover: File?) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionRepository.insertCollection(
                Collection(
                    id = 0,
                    name = name,
                    cover = cover,
                    mediaItemCount = 0
                )
            )
        }
    }

    fun deleteSelectedMediaItems() {
        val selected = _mediaItems.value.filter { it.isSelected }

        viewModelScope.launch(Dispatchers.IO) {
            selected.forEach {
                mediaItemRepository.deleteMediaItem(it)
            }
            onRefresh()
        }
    }

    fun duplicateSelectedMediaItems() {
        val selected = _mediaItems.value.filter { it.isSelected }

        viewModelScope.launch(Dispatchers.IO) {
            selected.forEach {
                mediaItemRepository.insertMediaItem(it.copy(id = 0))
            }
            onRefresh()
        }
    }

    fun addSelectedMediaItemsToCollection(collection: Collection) {
        val selected = _mediaItems.value.filter { it.isSelected }

        viewModelScope.launch(Dispatchers.IO) {
            selected.forEach {
                collectionRepository.insertCollectionEntry(collection, it)
            }
            onRefresh()
        }
    }

    fun onMassImport(context: Context, uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            uris.forEach { uri ->
                val path = DeviceTools.copyToInternalStorage(context, uri)
                val mediaItem = MediaItem(
                    id = 0L,
                    src = path,
                    caption = "",
                    description = "",
                    tags = emptyList()
                )
                mediaItemRepository.insertMediaItem(mediaItem)
                if (_selectedCollection.value.id != -1L) collectionRepository.insertCollectionEntry(
                    _selectedCollection.value,
                    mediaItem
                )
            }
            onRefresh()
        }
    }

    fun updateCollection(context: Context, collectionId: Long, newName: String?, newCover: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            val collection = collections.value.find { it.id == collectionId } ?: return@launch
            val file = newCover?.let { DeviceTools.copyToInternalStorage(context, it) }
            collection.cover?.let { DeviceTools.deleteFromInternalStorage(it) }

            collectionRepository.updateCollection(
                collection.copy(
                    name = newName ?: collection.name,
                    cover = file
                )
            )
        }
    }

    fun deleteCollection(collection: Collection) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionRepository.deleteCollection(collection)

            if (collection.id == _selectedCollection.value.id) {
                _selectedCollection.value = allCollection

                onRefresh()
            }
        }
    }

    fun exportCollection(context: Context, collection: Collection, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                CollectionTransferTool.exportCollection(
                    context = context,
                    collection = collection,
                    mediaItems = mediaItemRepository.getMediaWithTagsByCollection(collection.id),
                    outputUri = uri
                )
            }
            val toastText = context.getString(R.string.toast_collection_exported)
            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
        }

    }

    fun importCollection(context: Context, uri: Uri) {
        try {
            var toastText = ""
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    val (manifest, tempDir) = CollectionTransferTool.unzipCollection(context, uri)

                    val collectionCover = manifest.collection.coverSrc?.let {
                        DeviceTools.copyToInternalStorage(context, File(tempDir, it))
                    }
                    val collection = collectionRepository.insertCollection(
                        manifest.collection.toCollection(collectionCover)
                    )

                    manifest.mediaItems.forEach { mediaItem ->
                        val src = mediaItem.src?.let {
                            DeviceTools.copyToInternalStorage(
                                context,
                                File(tempDir, it)
                            )
                        }

                        val item = mediaItemRepository.insertMediaItem(mediaItem.toMediaItem(src))
                        collectionRepository.insertCollectionEntryByIds(collection, item)
                    }
                    toastText =
                        "${context.getString(R.string.import_successful)}: ${manifest.collection.name}"
                }
                Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                onRefresh()
            }
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.import_error), Toast.LENGTH_LONG)
                .show()
        }
    }

    fun onCodeRedeemed(context: Context, code: String) {
        if (code == "7777-7777-7777-7777") {
            Toast.makeText(
                context,
                context.getString(R.string.subscription_activation_successful),
                Toast.LENGTH_LONG
            ).show()

            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("isPremium", true).apply()
            _isPremium.value = true
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.subscription_activation_failed),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}