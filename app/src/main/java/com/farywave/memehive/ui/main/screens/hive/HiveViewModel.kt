package com.farywave.memehive.ui.main.screens.hive


import android.util.Log
import androidx.lifecycle.ViewModel
import com.farywave.memehive.data.local.db.entity.Collection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HiveViewModel : ViewModel() {
    private val _collections = MutableStateFlow(listOf(Collection(-1, "All", -1, mutableListOf())))
    val collections = _collections.asStateFlow()

    private val _selectedCollection = MutableStateFlow(_collections.value.first())
    val selectedCollection = _selectedCollection.asStateFlow()


    fun loadMemes() {
        _collections.value = listOf(
            Collection(-1, "All", -1, mutableListOf()),
            Collection(1, "John Pork", -1, mutableListOf()),
            Collection(2, "IShowSpeed", -1, mutableListOf()),
            Collection(3, "Ambatukam", -1, mutableListOf()),
            Collection(4, "Anime", -1, mutableListOf()),
        )
    }

    fun onSearch(query: String) {
        Log.d("HiveViewModel", "onSearch: $query")
    }

    fun onCollectionSelected(collection: Collection) {
        _selectedCollection.value = collection
        Log.d("HiveViewModel", "onCollectionSelected: $collection")
    }
}