package com.farywave.memehive.ui.activity.main.screen.hive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.Factory
import com.farywave.memehive.ui.use_case.CollectionTransferUseCases
import com.farywave.memehive.ui.use_case.CollectionUseCases
import com.farywave.memehive.ui.use_case.MediaItemUseCases
import com.farywave.memehive.ui.use_case.MediaSearchUseCase

class HiveViewModelFactory(
    private val mediaItemUseCases: MediaItemUseCases,
    private val collectionUseCases: CollectionUseCases,
    private val mediaSearchUseCase: MediaSearchUseCase,
    private val collectionTransferUseCases: CollectionTransferUseCases
) : Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HiveViewModel(
            mediaItemUseCases,
            collectionUseCases,
            mediaSearchUseCase,
            collectionTransferUseCases
        ) as T
    }
}