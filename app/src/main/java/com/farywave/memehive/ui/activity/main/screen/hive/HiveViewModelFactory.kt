package com.farywave.memehive.ui.activity.main.screen.hive

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.Factory
import com.farywave.memehive.core.collection_transfer.CollectionTransferTool
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.ImageRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.data.local.db.repository.TagRepository
import com.farywave.memehive.data.local.db.repository.TrigramRepository
import com.farywave.memehive.ui.use_case.CollectionTransferUseCases
import com.farywave.memehive.ui.use_case.CollectionUseCases
import com.farywave.memehive.ui.use_case.MediaItemUseCases
import com.farywave.memehive.ui.use_case.media_item_search.MediaSearchUseCase

class HiveViewModelFactory : Factory {

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

        val mediaItemRepo = MediaItemRepository(
            mediaItemDao
        )
        val collectionRepo = CollectionRepository(
            collectionDao,
            collectionEntryDao
        )
        val imageRepo = ImageRepository(context, imageDao)
        val tagRepo = TagRepository(tagDao, mediaItemTagDao)
        val trigramRepo = TrigramRepository(mediaItemTrigramDao)

        val mediaItemUseCases = MediaItemUseCases(
            db,
            mediaItemRepo,
            tagRepo,
            collectionRepo,
            trigramRepo,
            imageRepo
        )
        val collectionUseCases = CollectionUseCases(db,collectionRepo, imageRepo)
        val collectionTransferTool = CollectionTransferTool(context)
        val collectionTransferUseCases =
            CollectionTransferUseCases(
                db,
                collectionTransferTool,
                collectionUseCases,
                mediaItemUseCases
            )
        val mediaSearchUseCase =
            MediaSearchUseCase(mediaItemRepo)

        return HiveViewModel(
            mediaItemUseCases,
            collectionUseCases,
            mediaSearchUseCase,
            collectionTransferUseCases
        ) as T
    }
}