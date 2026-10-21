package com.farywave.memehive

import android.content.Context
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.ImageRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.data.local.db.repository.TagRepository
import com.farywave.memehive.data.local.db.repository.TrigramRepository
import com.farywave.memehive.data.local.storage.ImageStorage
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

    private val imageStorage = ImageStorage(appContext, ioDispatcher)
    private val imageRepo = ImageRepository(appContext, imageDao, ioDispatcher, imageStorage)
    private val mediaItemRepo = MediaItemRepository(mediaItemDao, imageRepo::toModel)
    private val collectionRepo = CollectionRepository(
        collectionDao,
        imageRepo::toModel,
        collectionEntryDao
    )
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
    val collectionTransferUseCases =
        CollectionTransferUseCases(
            database,
            appContext,
            ioDispatcher,
            collectionUseCases,
            mediaItemUseCases,
            imageRepo
        )
    val mediaSearchUseCase =
        MediaSearchUseCase(mediaItemRepo, computationDispatcher)
}
