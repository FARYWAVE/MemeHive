package com.farywave.memehive.ui.activity.main.screen.editing

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farywave.memehive.MemeHiveApplication
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.ui.activity.main.dialog.collection_editor.CollectionEditorViewModel
import com.farywave.memehive.ui.component.LocalAppFeedback
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter

@Composable
fun EditingRoute(
    initialUri: Uri?,
    onCopyImage: (Image) -> Unit,
    mediaItemId: Long,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val feedback = LocalAppFeedback.current

    val container = (context.applicationContext as MemeHiveApplication).container
    val factory = container.viewModelFactory(mediaItemId)
    val viewModel: EditingViewModel = viewModel(factory = factory)

    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val mediaItemDraft by viewModel.mediaItemDraft.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, initialUri) {
        initialUri?.let(viewModel::updateImage)
    }

    val contentState by viewModel.contentState.collectAsStateWithLifecycle()

    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(error?.id) {
        val currentError = error ?: return@LaunchedEffect
        feedback.show(currentError.messageResId)
        viewModel.clearError(currentError.id)
    }

    val collectionEditor: CollectionEditorViewModel = viewModel(factory = factory)
    val collectionEditorState by collectionEditor.state.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val loadError by viewModel.loadError.collectAsStateWithLifecycle()
    val hasCollectionsLoadError by viewModel.hasCollectionsLoadError.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle, onClose) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            combine(viewModel.isDeleted, viewModel.closeRequested) { deleted, saved ->
                    deleted || saved
                }
                .filter { it }
                .collect { onClose() }
        }
    }

    EditingScreen(
        state =
            EditingUiState(
                mediaItemDraft = mediaItemDraft,
                collections = collections,
                contentState = contentState,
                collectionEditor = collectionEditorState,
                isSaving = isSaving,
                isLoading = isLoading,
                loadError = loadError,
                hasCollectionsLoadError = hasCollectionsLoadError,
            ),
        onBack = onClose,
        onSave = { viewModel.saveCurrentItem() },
        onSaveAndClose = { viewModel.saveCurrentItem(closeAfterSave = true) },
        onRetryLoading = viewModel::retryLoading,
        onRetryCollections = viewModel::loadCollections,
        onDiscard = viewModel::discardChanges,
        onDuplicate = viewModel::duplicateCurrentItem,
        onDelete = viewModel::deleteCurrentItem,
        onCreateCollection = { collectionEditor.open() },
        onSubmitCollection = collectionEditor::submit,
        onDismissCollection = collectionEditor::dismiss,
        onToggleCollection = viewModel::toggleCollection,
        onSetAsCover = viewModel::setCurrentItemAsCover,
        onImageChange = viewModel::updateImage,
        onCaptionChange = viewModel::updateCaption,
        onDescriptionChange = viewModel::updateDescription,
        onRemoveTag = viewModel::removeTag,
        onUpdateTag = viewModel::updateTag,
        onAddTag = viewModel::addTag,
        onCopyImage = onCopyImage,
    )
}
