package com.farywave.memehive.ui.activity.picker

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.ImageRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.data.local.db.repository.TagRepository
import com.farywave.memehive.data.local.db.repository.TrigramRepository
import com.farywave.memehive.ui.use_case.CollectionUseCases
import com.farywave.memehive.ui.use_case.media_item_search.MediaSearchUseCase

class PickerViewModelFactory: ViewModelProvider.Factory {

    private val context: Context

    constructor(context: Context) {
        this.context = context
}

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = MemeHiveDatabase.getInstance(context)

        val mediaItemDao = db.mediaItemDao()
        val collectionDao = db.collectionDao()
        val collectionEntryDao = db.collectionEntryDao()
        val imageDao = db.imageDao()
        val tagDao = db.tagDao()
        val mediaItemTagDao = db.mediaItemTagDao()
        val mediaItemTrigramDao = db.mediaItemTrigramDao()

        val collectionRepo = CollectionRepository(
            collectionDao,
            collectionEntryDao
        )
        val imageRepo = ImageRepository(context, imageDao)
        val tagRepo = TagRepository(tagDao, mediaItemTagDao)
        val trigramRepo = TrigramRepository(mediaItemTrigramDao)

        val collectionUseCases = CollectionUseCases(collectionRepo, imageRepo)
        val mediaSearchUseCase =
            MediaSearchUseCase(mediaItemDao, tagRepo, trigramRepo, collectionRepo)

        return PickerViewModel(collectionUseCases, mediaSearchUseCase) as T
    }
}