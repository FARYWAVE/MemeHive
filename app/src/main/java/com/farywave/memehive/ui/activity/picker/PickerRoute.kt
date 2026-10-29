package com.farywave.memehive.ui.activity.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farywave.memehive.MemeHiveApplication
import com.farywave.memehive.domain.model.MediaItem
import com.farywave.memehive.ui.component.LocalAppFeedback

@Composable
fun PickerRoute(onItemClicked: (MediaItem) -> Unit) {
    val context = LocalContext.current
    val feedback = LocalAppFeedback.current

    val viewModel: PickerViewModel =
        (context.applicationContext as MemeHiveApplication).container.let {
            viewModel(
                factory = PickerViewModelFactory(it.collectionUseCases, it.mediaSearchUseCase)
            )
        }

    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(error?.id) {
        val currentError = error ?: return@LaunchedEffect
        feedback.show(currentError.messageResId)
        viewModel.clearError(currentError.id)
    }

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val mediaState by viewModel.mediaState.collectAsStateWithLifecycle()
    val collectionsLoading by viewModel.collectionsLoading.collectAsStateWithLifecycle()
    val collectionsFailed by viewModel.collectionsFailed.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val selectedCollectionId by viewModel.selectedCollectionId.collectAsStateWithLifecycle()

    PickerScreen(
        state = PickerUiState(
            searchQuery = searchQuery,
            mediaItems = mediaState.items,
            isLoading = mediaState.isLoading || collectionsLoading,
            loadFailed = mediaState.failure != null || collectionsFailed,
            collections = collections,
            selectedCollectionId = selectedCollectionId,
        ),
        onRetryLoading = viewModel::retryLoading,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onCollectionSelected = viewModel::onCollectionSelected,
        onItemClicked = { item ->
            onItemClicked(item)
        },
    )
}
