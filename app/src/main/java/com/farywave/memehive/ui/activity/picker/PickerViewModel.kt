package com.farywave.memehive.ui.activity.picker

import com.farywave.memehive.ui.model.UiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import android.util.Log
import androidx.annotation.StringRes
import com.farywave.memehive.R
import kotlinx.coroutines.flow.update
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.ui.use_case.CollectionUseCases
import com.farywave.memehive.ui.use_case.MediaSearchUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PickerViewModel(
    private val collectionUseCases: CollectionUseCases,
    private val mediaSearchUseCase: MediaSearchUseCase
) : ViewModel() {
    private val _error = MutableStateFlow<UiError?>(null)
    val error = _error.asStateFlow()

    private val _collections = MutableStateFlow<List<Collection>>(emptyList())
    val collections = _collections.asStateFlow()

    private val _selectedCollectionId = MutableStateFlow<Long?>(null)
    val selectedCollectionId = _selectedCollectionId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val mediaItems = mediaSearchUseCase(selectedCollectionId, searchQuery)
        .map { items -> items.filter { it.image != null } }
        .catch { reportError(R.string.media_load_error, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadCollections()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCollectionSelected(collectionId: Long?) {
        _selectedCollectionId.value = collectionId
    }

    fun reportCopyFailure() {
        _error.value = UiError(R.string.image_copy_failed)
    }

    fun clearError(id: String) {
        _error.update { current -> current?.takeUnless { it.id == id } }
    }

    private fun loadCollections() {
        viewModelScope.launch {
            collectionUseCases.getAllCollections()
                .catch { reportError(R.string.collections_load_error, it) }
                .collect { collections ->
                    _collections.value = collections
                    val selectedId = _selectedCollectionId.value
                    if (selectedId != null && collections.none { it.id == selectedId }) {
                        _selectedCollectionId.value = null
                    }
                }
        }
    }

    private fun reportError(@StringRes message: Int, failure: Throwable) {
        if (failure is CancellationException) throw failure
        Log.w("PickerViewModel", "Screen operation failed", failure)
        _error.value = UiError(message)
    }
}
