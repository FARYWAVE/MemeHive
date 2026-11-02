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
fun PickerRoute(onItemClick: (MediaItem) -> Unit) {
    val context = LocalContext.current
    val feedback = LocalAppFeedback.current

    val container = (context.applicationContext as MemeHiveApplication).container
    val factory = container.viewModelFactory()
    val viewModel: PickerViewModel = viewModel(factory = factory)

    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(error?.id) {
        val currentError = error ?: return@LaunchedEffect
        feedback.show(currentError.messageResId)
        viewModel.clearError(currentError.id)
    }

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val mediaState by viewModel.mediaState.collectAsStateWithLifecycle()
    val isLoadingCollections by viewModel.isLoadingCollections.collectAsStateWithLifecycle()
    val hasCollectionsLoadError by viewModel.hasCollectionsLoadError.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val selectedCollectionId by viewModel.selectedCollectionId.collectAsStateWithLifecycle()

    PickerScreen(
        state =
            PickerUiState(
                searchQuery = searchQuery,
                mediaItems = mediaState.items,
                isLoading = mediaState.isLoading || isLoadingCollections,
                loadFailed = mediaState.failure != null || hasCollectionsLoadError,
                collections = collections,
                selectedCollectionId = selectedCollectionId,
            ),
        onRetryLoading = viewModel::retryLoading,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onCollectionSelected = viewModel::onCollectionSelected,
        onItemClick = { item ->
            onItemClick(item)
        },
    )
}
