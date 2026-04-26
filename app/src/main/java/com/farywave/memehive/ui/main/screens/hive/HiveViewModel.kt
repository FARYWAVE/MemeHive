package com.farywave.memehive.ui.main.screens.hive


import android.util.Log
import androidx.lifecycle.ViewModel
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HiveViewModel : ViewModel() {
    private val _collections = MutableStateFlow(listOf(Collection(-1, "All", -1, mutableListOf())))
    val collections = _collections.asStateFlow()

    private val _selectedCollection = MutableStateFlow(_collections.value.first())
    val selectedCollection = _selectedCollection.asStateFlow()

    private val _mediaItems = MutableStateFlow(emptyList<MediaItem>())
    val mediaItems = _mediaItems.asStateFlow()


    fun loadCollections() {
        _collections.value = listOf(
            Collection(-1, "All", -1, mutableListOf()),
            Collection(1, "John Pork", -1, mutableListOf()),
            Collection(2, "IShowSpeed", -1, mutableListOf()),
            Collection(3, "Games", -1, mutableListOf()),
            Collection(4, "Ambatukam", -1, mutableListOf()),
        )
    }

    fun loadMediaItems() {
        _mediaItems.value =
            (0..100).map {
                MediaItem(
                    it,
                    null,
                    "Name $it",
                    "Description $it",
                    (0..it).map { "tag$it" }.toMutableList(),
                    it % 3 == 1
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
}