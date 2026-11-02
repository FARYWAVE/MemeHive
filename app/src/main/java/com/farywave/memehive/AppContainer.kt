package com.farywave.memehive

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory as buildViewModelFactory
import com.farywave.memehive.application.usecase.CollectionTransferUseCases
import com.farywave.memehive.application.usecase.CollectionUseCases
import com.farywave.memehive.application.usecase.DeleteMediaItemsUseCase
import com.farywave.memehive.application.usecase.MediaItemUseCases
import com.farywave.memehive.application.usecase.MediaSearchUseCase
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.ImageRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.data.local.db.repository.TagRepository
import com.farywave.memehive.data.local.storage.ImageStorage
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.ui.activity.main.dialog.collection_editor.CollectionDraft
import com.farywave.memehive.ui.activity.main.dialog.collection_editor.CollectionEditorViewModel
import com.farywave.memehive.ui.activity.main.screen.editing.EditingViewModel
import com.farywave.memehive.ui.activity.main.screen.hive.HiveViewModel
import com.farywave.memehive.ui.activity.picker.PickerViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

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

    private val imageStorage = ImageStorage(appContext, ioDispatcher)
    private val imageRepository = ImageRepository(appContext, imageDao, ioDispatcher, imageStorage)
    private val mediaItemRepository = MediaItemRepository(mediaItemDao, imageRepository::toModel)
    private val collectionRepository =
        CollectionRepository(
            collectionDao,
            imageRepository::toModel,
            collectionEntryDao,
        )
    private val tagRepository = TagRepository(tagDao, mediaItemTagDao)

    private val deleteMediaItemsUseCase =
        DeleteMediaItemsUseCase(database, mediaItemRepository, tagRepository, imageRepository)

    val mediaItemUseCases =
        MediaItemUseCases(
            database,
            mediaItemRepository,
            tagRepository,
            collectionRepository,
            imageRepository,
            deleteMediaItemsUseCase,
        )
    val collectionUseCases =
        CollectionUseCases(database, collectionRepository, imageRepository, deleteMediaItemsUseCase)
    val collectionTransferUseCases =
        CollectionTransferUseCases(
            database,
            appContext,
            ioDispatcher,
            collectionUseCases,
            mediaItemUseCases,
            imageRepository,
        )
    val mediaSearchUseCase = MediaSearchUseCase(mediaItemRepository, computationDispatcher)

    fun viewModelFactory(mediaItemId: Long = 0L) = buildViewModelFactory {
        initializer {
            HiveViewModel(
                mediaItemUseCases,
                collectionUseCases,
                collectionTransferUseCases,
                mediaSearchUseCase,
            )
        }
        initializer { PickerViewModel(collectionUseCases, mediaSearchUseCase) }
        initializer {
            EditingViewModel(
                mediaItemId,
                mediaItemUseCases,
                collectionUseCases,
                createSavedStateHandle(),
            )
        }
        initializer {
            CollectionEditorViewModel(createSavedStateHandle(), ::saveCollectionDraft)
        }
    }

    private suspend fun saveCollectionDraft(id: Long?, draft: CollectionDraft) {
        try {
            val cover = draft.coverUri?.let { Image(uri = it.toUri()) }
            if (id == null) {
                collectionUseCases.createCollection(Collection(name = draft.name, cover = cover))
            } else {
                val original =
                    collectionUseCases.observeCollections().first().firstOrNull { it.id == id }
                        ?: error("Collection $id no longer exists")
                collectionUseCases.updateCollection(original.copy(name = draft.name, cover = cover))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Log.w("CollectionEditorDialog", "Could not save collection", failure)
            throw failure
        }
    }
}
