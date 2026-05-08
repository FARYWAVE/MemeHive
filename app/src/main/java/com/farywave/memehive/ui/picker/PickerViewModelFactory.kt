package com.farywave.memehive.ui.picker

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository

class PickerViewModelFactory: ViewModelProvider.Factory {

    private val context: Context

    constructor(context: Context) {
        this.context = context
}

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = MemeHiveDatabase.getInstance(context)

        val mediaItemRepo = MediaItemRepository(
            db.mediaItemDao(),
            db.mediaItemTagDao(),
            db.tagDao(),
            db.mediaItemTrigramDao(),
            db.collectionEntryDao()
        )
        val collectionRepo = CollectionRepository(
            db.collectionDao(),
            db.collectionEntryDao()
        )

        return PickerViewModel(mediaItemRepo, collectionRepo) as T
    }
}