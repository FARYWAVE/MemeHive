package com.farywave.memehive.ui.activity.main.screen.hive

import android.database.sqlite.SQLiteException
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farywave.memehive.R
import com.farywave.memehive.core.collection_transfer.InvalidCollectionArchiveException
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.domain.model.MediaItem
import com.farywave.memehive.ui.model.UiError
import com.farywave.memehive.ui.use_case.CollectionTransferUseCases
import com.farywave.memehive.ui.use_case.CollectionUseCases
import com.farywave.memehive.ui.use_case.MediaItemUseCases
import com.farywave.memehive.ui.use_case.MediaSearchState
import com.farywave.memehive.ui.use_case.MediaSearchUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HiveViewModel(
    private val mediaItemUseCases: MediaItemUseCases,
    private val collectionUseCases: CollectionUseCases,
    private val mediaSearchUseCase: MediaSearchUseCase,
    private val collectionTransferUseCases: CollectionTransferUseCases
) : ViewModel() {
    private val _error = MutableStateFlow<UiError?>(null)
    val error = _error.asStateFlow()

    private val _transferMessages = MutableStateFlow<List<CollectionTransferMessage>>(emptyList())
    val transferMessages = _transferMessages.asStateFlow()

    private var nextTransferMessageId = 0L

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

    private val _collectionsLoading = MutableStateFlow(true)
    val collectionsLoading = _collectionsLoading.asStateFlow()

    private val _collectionsFailed = MutableStateFlow(false)
    val collectionsFailed = _collectionsFailed.asStateFlow()

    private val retries = MutableStateFlow(0)
    val mediaState = mediaSearchUseCase.observeState(
        selectedCollectionId, searchQuery.debounce(200), retries,
    )
        .onEach { state ->
            if (!state.isLoading && state.failure == null) {
                val visibleIds = state.items.mapTo(mutableSetOf()) { it.id }
                _selectedMediaItemIds.update { it.intersect(visibleIds) }
                if (_selectedMediaItemIds.value.isEmpty()) _isMassEditingMode.value = false
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MediaSearchState(isLoading = true))

    fun retryLoading() {
        _collectionsLoading.value = true
        _collectionsFailed.value = false
        retries.update { it + 1 }
    }

    init {
        loadCollections()
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

    fun enableMassEditingMode() {
        _isMassEditingMode.value = true
    }

    fun disableMassEditingMode() {
        _isMassEditingMode.value = false
        _selectedMediaItemIds.value = emptySet()
    }

    fun selectItem(itemId: Long) {
        _selectedMediaItemIds.value += itemId
    }

    fun toggleItemSelection(itemId: Long) {
        if (itemId in _selectedMediaItemIds.value) _selectedMediaItemIds.value -= itemId
        else _selectedMediaItemIds.value += itemId
        if (_selectedMediaItemIds.value.isEmpty()) disableMassEditingMode()
    }

    fun onMassImport(uris: List<Uri>) {
        val collectionId = _selectedCollectionId.value
        launchWithError(R.string.media_import_error) {
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

    fun duplicateSelectedMediaItems() {
        val selected = mediaState.value.items.filter { it.id in _selectedMediaItemIds.value }

        launchWithError(R.string.media_duplicate_error) {
            selected.forEach {
                mediaItemUseCases.createMediaItem(it.copy(id = 0))
            }
        }
    }

    fun addSelectedMediaItemsToCollection(collection: Collection) {
        val selectedIds = _selectedMediaItemIds.value
        launchWithError(R.string.collection_add_items_error) {
            collectionUseCases.addItemsToCollection(selectedIds, collection.id)
        }
    }

    fun deleteSelectedMediaItems() {
        val selectedIds = _selectedMediaItemIds.value
        launchWithError(R.string.media_delete_error) {
            mediaItemUseCases.deleteMediaItemsByIds(selectedIds)
        }
    }

    fun deleteCollection(collection: Collection) {
        launchWithError(R.string.collection_delete_error) {
            collectionUseCases.deleteCollection(collection)
        }
    }

    fun importCollection(uri: Uri) {
        launchCollectionTransfer(R.string.import_successful) {
            collectionTransferUseCases.importCollection(uri)
        }
    }

    fun exportCollection(collection: Collection, uri: Uri) {
        launchCollectionTransfer(R.string.toast_collection_exported) {
            collectionTransferUseCases.exportCollection(collection, uri)
        }
    }

    fun onTransferMessageShown(id: Long) {
        _transferMessages.update { messages -> messages.filterNot { it.id == id } }
    }

    fun clearError(id: String) {
        _error.update { current -> current?.takeUnless { it.id == id } }
    }

    private fun loadCollections() {
        viewModelScope.launch {
            retries.flatMapLatest {
                collectionUseCases.getAllCollections()
                    .catch {
                        _collectionsLoading.value = false
                        _collectionsFailed.value = true
                        reportError(R.string.collections_load_error, it)
                    }
            }
                .collect { collections ->
                    _collectionsLoading.value = false
                    _collectionsFailed.value = false
                    _collections.value = collections
                    val selectedId = _selectedCollectionId.value
                    if (selectedId != null && collections.none { it.id == selectedId }) {
                        onCollectionSelected(null)
                    }
                }
        }
    }

    private fun launchCollectionTransfer(
        @StringRes successMessage: Int,
        operation: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                operation()
                addTransferMessage(successMessage)
            } catch (error: InvalidCollectionArchiveException) {
                reportTransferFailure(R.string.import_invalid_archive, error)
            } catch (error: IOException) {
                reportTransferFailure(R.string.collection_transfer_io_error, error)
            } catch (error: SecurityException) {
                reportTransferFailure(R.string.collection_transfer_permission_error, error)
            } catch (error: SQLiteException) {
                reportTransferFailure(R.string.collection_transfer_database_error, error)
            }
        }
    }

    private fun addTransferMessage(@StringRes message: Int) {
        val notification = CollectionTransferMessage(nextTransferMessageId++, message)
        _transferMessages.update { it + notification }
    }

    private fun reportTransferFailure(@StringRes message: Int, error: Exception) {
        reportError(message, error)
    }

    private fun launchWithError(@StringRes message: Int, operation: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                reportError(message, failure)
            }
        }
    }

    private fun reportError(@StringRes message: Int, failure: Throwable) {
        if (failure is CancellationException) throw failure
        Log.w("HiveViewModel", "Screen operation failed", failure)
        _error.value = UiError(message)
    }


}

data class CollectionTransferMessage(val id: Long, @param:StringRes val textRes: Int)
