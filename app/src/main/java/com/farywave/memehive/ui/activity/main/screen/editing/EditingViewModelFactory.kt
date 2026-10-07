package com.farywave.memehive.ui.activity.main.screen.editing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.*
import com.farywave.memehive.MemeHiveApplication
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.ImageRepository
import com.farywave.memehive.data.local.db.repository.MediaItemRepository
import com.farywave.memehive.data.local.db.repository.TagRepository
import com.farywave.memehive.data.local.db.repository.TrigramRepository
import com.farywave.memehive.ui.use_case.CollectionUseCases
import com.farywave.memehive.ui.use_case.MediaItemUseCases

class EditingViewModelFactory(
    private val mediaItemId: Long,
    private val mediaItemUseCases: MediaItemUseCases,
    private val collectionUseCases: CollectionUseCases,
) : Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditingViewModel(
            mediaItemId,
            mediaItemUseCases,
            collectionUseCases
        ) as T
    }
}