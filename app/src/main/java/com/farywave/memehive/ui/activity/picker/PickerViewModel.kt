package com.farywave.memehive.ui.activity.picker

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
    private val _collections = MutableStateFlow<List<Collection>>(emptyList())
    val collections = _collections.asStateFlow()

    private val _selectedCollectionId = MutableStateFlow<Long?>(null)
    val selectedCollectionId = _selectedCollectionId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val mediaItems = mediaSearchUseCase(selectedCollectionId, searchQuery)
        .map { items -> items.filter { it.image != null } }
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
                    _selectedCollectionId.value = null
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCollectionSelected(collectionId: Long?) {
        _selectedCollectionId.value = collectionId
    }
}
