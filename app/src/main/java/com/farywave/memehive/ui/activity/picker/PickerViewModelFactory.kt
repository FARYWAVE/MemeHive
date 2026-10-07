package com.farywave.memehive.ui.activity.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.farywave.memehive.ui.use_case.CollectionUseCases
import com.farywave.memehive.ui.use_case.MediaSearchUseCase

class PickerViewModelFactory(
    private val collectionUseCases: CollectionUseCases,
    private val mediaSearchUseCase: MediaSearchUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PickerViewModel(collectionUseCases, mediaSearchUseCase) as T
    }
}