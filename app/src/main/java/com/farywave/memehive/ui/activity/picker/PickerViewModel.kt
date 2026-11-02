package com.farywave.memehive.ui.activity.picker

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farywave.memehive.R
import com.farywave.memehive.application.usecase.CollectionUseCases
import com.farywave.memehive.application.usecase.MediaSearchState
import com.farywave.memehive.application.usecase.MediaSearchUseCase
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.ui.model.UiMessageQueue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PickerViewModel(
    private val collectionUseCases: CollectionUseCases,
    mediaSearchUseCase: MediaSearchUseCase,
) : ViewModel() {
    private val messages = UiMessageQueue()
    val error = messages.current

    private val _collections = MutableStateFlow<List<Collection>>(emptyList())
    val collections = _collections.asStateFlow()

    private val _selectedCollectionId = MutableStateFlow<Long?>(null)
    val selectedCollectionId = _selectedCollectionId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoadingCollections = MutableStateFlow(true)
    val isLoadingCollections = _isLoadingCollections.asStateFlow()

    private val _hasCollectionsLoadError = MutableStateFlow(false)
    val hasCollectionsLoadError = _hasCollectionsLoadError.asStateFlow()

    private val retries = MutableStateFlow(0)
    val mediaState =
        mediaSearchUseCase
            .observeState(
                selectedCollectionId,
                searchQuery.debounce(200.milliseconds),
                retries,
            )
            .map { it.copy(items = it.items.filter { item -> item.image != null }) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                MediaSearchState(isLoading = true),
            )

    fun retryLoading() {
        _isLoadingCollections.value = true
        _hasCollectionsLoadError.value = false
        retries.update { it + 1 }
    }

    init {
        loadCollections()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCollectionSelected(collectionId: Long?) {
        _selectedCollectionId.value = collectionId
    }

    fun clearError(id: String) {
        messages.dismiss(id)
    }

    private fun loadCollections() {
        viewModelScope.launch {
            retries
                .flatMapLatest {
                    collectionUseCases.observeCollections().catch {
                        _isLoadingCollections.value = false
                        _hasCollectionsLoadError.value = true
                        reportError(R.string.error_collections_load, it)
                    }
                }
                .collect { collections ->
                    _isLoadingCollections.value = false
                    _hasCollectionsLoadError.value = false
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
        messages.show(message)
    }
}
