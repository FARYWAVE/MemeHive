package com.farywave.memehive.ui.activity.main.screen.editing

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.farywave.memehive.ui.use_case.CollectionUseCases
import com.farywave.memehive.ui.use_case.MediaItemUseCases

class EditingViewModelFactory(
    private val mediaItemId: Long,
    private val mediaItemUseCases: MediaItemUseCases,
    private val collectionUseCases: CollectionUseCases,
    private val contentResolver: ContentResolver,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return EditingViewModel(
            mediaItemId,
            mediaItemUseCases,
            collectionUseCases,
            contentResolver,
            extras.createSavedStateHandle()
        ) as T
    }
}
