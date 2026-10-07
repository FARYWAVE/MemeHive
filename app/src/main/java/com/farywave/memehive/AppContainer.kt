package com.farywave.memehive

import android.content.Context
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
import com.farywave.memehive.ui.use_case.MediaSearchUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class AppContainer(
    context: Context,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val appContext = context.applicationContext
    private val database = MemeHiveDatabase.getInstance(appContext)

    private val mediaItemDao = database.mediaItemDao()
    private val collectionDao = database.collectionDao()
    private val collectionEntryDao = database.collectionEntryDao()
    private val imageDao = database.imageDao()
    private val tagDao = database.tagDao()
    private val mediaItemTagDao = database.mediaItemTagDao()
    private val mediaItemTrigramDao = database.mediaItemTrigramDao()

    private val mediaItemRepo = MediaItemRepository(
        mediaItemDao
    )
    private val collectionRepo = CollectionRepository(
        collectionDao,
        collectionEntryDao
    )
    private val imageRepo = ImageRepository(appContext, imageDao, ioDispatcher)
    private val tagRepo = TagRepository(tagDao, mediaItemTagDao)
    private val trigramRepo = TrigramRepository(mediaItemTrigramDao)

    val mediaItemUseCases = MediaItemUseCases(
        database,
        mediaItemRepo,
        tagRepo,
        collectionRepo,
        trigramRepo,
        imageRepo
    )
    val collectionUseCases = CollectionUseCases(database, collectionRepo, imageRepo)
    private val collectionTransferTool = CollectionTransferTool(appContext, ioDispatcher)
    val collectionTransferUseCases =
        CollectionTransferUseCases(
            database,
            collectionTransferTool,
            collectionUseCases,
            mediaItemUseCases,
            imageRepo
        )
    val mediaSearchUseCase =
        MediaSearchUseCase(mediaItemRepo, computationDispatcher)
}
