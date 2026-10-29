package com.farywave.memehive.ui.activity.main.screen.editing

import android.net.Uri
import android.widget.Toast
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
import kotlinx.coroutines.flow.filter

@Composable
fun EditingRoute(
    initialUri: Uri?,
    onCopyImage: (Image, onFailure: () -> Unit) -> Unit,
    mediaItemId: Long,
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    val viewModel: EditingViewModel =
        (context.applicationContext as MemeHiveApplication).container.let {
            viewModel(
                factory = EditingViewModelFactory(
                    mediaItemId,
                    it.mediaItemUseCases,
                    it.collectionUseCases,
                    context.applicationContext.contentResolver
                )
            )
        }

    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val mediaItemDraft by viewModel.mediaItemDraft.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, initialUri) {
        initialUri?.let(viewModel::updateImage)
    }

    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(error?.id) {
        val currentError = error ?: return@LaunchedEffect
        Toast.makeText(context, currentError.messageResId, Toast.LENGTH_LONG).show()
        viewModel.clearError(currentError.id)
    }

    val collectionEditor: CollectionEditorViewModel = viewModel(
        factory = CollectionEditorViewModel.Factory(
            (context.applicationContext as MemeHiveApplication).container.collectionUseCases
        )
    )
    val collectionEditorState by collectionEditor.state.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle, onClose) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.isDeleted.filter { it }.collect { onClose() }
        }
    }

    EditingScreen(
        state = EditingUiState(
            mediaItemDraft = mediaItemDraft,
            collections = collections,
            screenState = screenState,
            collectionEditor = collectionEditorState,
            isSaving = isSaving,
        ),
        onBack = onClose,
        onSave = viewModel::saveCurrentItem,
        onDiscard = viewModel::discardChanges,
        onDuplicate = viewModel::duplicateCurrentItem,
        onDelete = viewModel::deleteCurrentItem,
        onCreateCollection = { collectionEditor.open() },
        onSubmitCollection = collectionEditor::submit,
        onDismissCollection = collectionEditor::dismiss,
        onToggleCollection = viewModel::toggleCollection,
        onSetAsCover = viewModel::setCurrentItemAsCover,
        onImageChanged = viewModel::updateImage,
        onCaptionChanged = viewModel::updateName,
        onDescriptionChanged = viewModel::updateDescription,
        onRemoveTag = viewModel::removeTag,
        onUpdateTag = viewModel::updateTag,
        onAddTag = viewModel::addTag,
        onCopyImage = onCopyImage,
    )
}
