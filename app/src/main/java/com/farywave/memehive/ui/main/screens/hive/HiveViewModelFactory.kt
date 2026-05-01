package com.farywave.memehive.ui.main.screens.hive

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.Factory
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository

class HiveViewModelFactory: Factory {

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
            db.mediaItemTrigramDao()
        )
        val collectionRepo = CollectionRepository(
            db.collectionDao(),
            db.collectionEntryDao()
        )

        return HiveViewModel(mediaItemRepo, collectionRepo) as T
    }
}