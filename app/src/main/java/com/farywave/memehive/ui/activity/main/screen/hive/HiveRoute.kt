package com.farywave.memehive.ui.activity.main.screen.hive

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farywave.memehive.MemeHiveApplication
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.ui.activity.main.dialog.collection_editor.CollectionEditorViewModel


@Composable
fun HiveRoute(
    onEnableNotification: (Boolean) -> Unit,
    onOpenMedia: (Long?) -> Unit,
) {
    val context = LocalContext.current

    val viewModel: HiveViewModel =
        (context.applicationContext as MemeHiveApplication).container.let {
            viewModel(
                factory = HiveViewModelFactory(
                    it.mediaItemUseCases,
                    it.collectionUseCases,
                    it.mediaSearchUseCase,
                    it.collectionTransferUseCases
                )
            )
        }

    val error by viewModel.error.collectAsStateWithLifecycle()
    val transferMessages by viewModel.transferMessages.collectAsStateWithLifecycle()

    val transferMessage = transferMessages.firstOrNull()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importCollection(uri)
        }
    }

    LaunchedEffect(error?.id) {
        val currentError = error ?: return@LaunchedEffect
        Toast.makeText(context, currentError.messageResId, Toast.LENGTH_LONG).show()
        viewModel.clearError(currentError.id)
    }

    LaunchedEffect(transferMessage) {
        transferMessage?.let { message ->
            Toast.makeText(context, message.textRes, Toast.LENGTH_SHORT).show()
            viewModel.onTransferMessageShown(message.id)
        }
    }

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val mediaState by viewModel.mediaState.collectAsStateWithLifecycle()
    val collectionsLoading by viewModel.collectionsLoading.collectAsStateWithLifecycle()
    val collectionsFailed by viewModel.collectionsFailed.collectAsStateWithLifecycle()
    val selectedMediaItemIds by viewModel.selectedMediaItemIds.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val selectedCollectionId by viewModel.selectedCollectionId.collectAsStateWithLifecycle()
    val isMassEditingMode by viewModel.isMassEditingMode.collectAsStateWithLifecycle()
    val collectionEditor: CollectionEditorViewModel = viewModel(
        factory = CollectionEditorViewModel.Factory(
            (context.applicationContext as MemeHiveApplication).container.collectionUseCases
        )
    )
    val collectionEditorState by collectionEditor.state.collectAsStateWithLifecycle()

    val massImportLauncher = DeviceTools.requestMultipleMedia(viewModel::onMassImport)
    var collectionToExport by remember { mutableStateOf<Collection?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val collection = collectionToExport
        if (uri != null && collection != null) {
            viewModel.exportCollection(collection, uri)
        }
    }

    HiveScreen(
        state = HiveUiState(
            searchQuery = searchQuery,
            mediaItems = mediaState.items,
            isLoading = mediaState.isLoading || collectionsLoading,
            loadFailed = mediaState.failure != null || collectionsFailed,
            selectedMediaItemIds = selectedMediaItemIds,
            collections = collections,
            selectedCollectionId = selectedCollectionId,
            isMassEditingMode = isMassEditingMode,
            collectionEditor = collectionEditorState
        ),
        onCreateMedia = { onOpenMedia(null) },
        onOpenMedia = { id -> onOpenMedia(id) },
        onCreateCollection = { collectionEditor.open() },
        onEditCollection = collectionEditor::open,
        onSubmitCollection = collectionEditor::submit,
        onDismissCollection = collectionEditor::dismiss,
        onEnableNotification = onEnableNotification,
        onMassImport = { massImportLauncher.launch("image/*") },
        onCollectionImport = { importLauncher.launch(arrayOf("application/zip")) },
        onExportCollection = { collection ->
            collectionToExport = collection
            exportLauncher.launch("${collection.name}.zip")
        },
        onDeleteCollection = viewModel::deleteCollection,
        onRetryLoading = viewModel::retryLoading,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onCollectionSelected = viewModel::onCollectionSelected,
        onToggleMediaSelection = viewModel::toggleItemSelection,
        onStartMediaSelection = { id ->
            viewModel.enableMassEditingMode()
            viewModel.selectItem(id)
        },
        onCancelMediaSelection = viewModel::disableMassEditingMode,
        onDeleteSelectedMedia = {
            viewModel.deleteSelectedMediaItems()
            viewModel.disableMassEditingMode()
        },
        onDuplicateSelectedMedia = {
            viewModel.duplicateSelectedMediaItems()
            viewModel.disableMassEditingMode()
        },
        onAddSelectedMediaToCollection = { collection ->
            viewModel.addSelectedMediaItemsToCollection(collection)
            viewModel.disableMassEditingMode()
        },
    )
}
