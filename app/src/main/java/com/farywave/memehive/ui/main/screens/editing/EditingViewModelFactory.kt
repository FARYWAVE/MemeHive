package com.farywave.memehive.ui.main.screens.editing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.*
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository

class EditingViewModelFactory : Factory {

    private val context: Context
    private val mediaItemId: Long

    constructor(context: Context, mediaItemId: Long) {
        this.context = context
        this.mediaItemId = mediaItemId
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

        return EditingViewModel(
            mediaItemId,
            mediaItemRepo,
            collectionRepo
        ) as T
    }
}