package com.farywave.memehive.ui.activity.main.screen.hive

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.farywave.memehive.R
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.ui.activity.main.dialog.AboutAppDialog
import com.farywave.memehive.ui.activity.main.dialog.DeleteCollectionDialog
import com.farywave.memehive.ui.activity.main.dialog.DeleteSelectedMediaItemsDialog
import com.farywave.memehive.ui.activity.main.dialog.collection_editor.CollectionDraft
import com.farywave.memehive.ui.activity.main.dialog.collection_editor.CollectionEditorDialog
import com.farywave.memehive.ui.component.AppActionMenu
import com.farywave.memehive.ui.component.AppIconButton
import com.farywave.memehive.ui.component.CollectionPickerSheet
import com.farywave.memehive.ui.component.EmptyHiveMessage
import com.farywave.memehive.ui.component.EmptySearchResultMessage
import com.farywave.memehive.ui.component.SearchAndCollectionBar
import com.farywave.memehive.ui.component.SelectableMediaItemCard
import com.farywave.memehive.ui.theme.AppTypography
import com.farywave.memehive.ui.theme.LocalAppColors
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@Composable
fun HiveScreen(
    state: HiveUiState,
    onStartupReady: () -> Unit = {},
    onCreateMedia: () -> Unit,
    onOpenMedia: (Long) -> Unit,
    onCreateCollection: () -> Unit,
    onSubmitCollection: (CollectionDraft) -> Unit,
    onDismissCollection: () -> Unit,
    onEditCollection: (Collection) -> Unit,
    onEnableNotification: (Boolean) -> Unit,
    importImages: () -> Unit,
    onCollectionImport: () -> Unit,
    onExportCollection: (Collection) -> Unit,
    onDeleteCollection: (collection: Collection, deleteContents: Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRetryLoading: () -> Unit = {},
    onCollectionSelected: (Long?) -> Unit,
    onToggleMediaSelection: (Long) -> Unit,
    onStartMediaSelection: (Long) -> Unit,
    onCancelMediaSelection: () -> Unit,
    onDeleteSelectedMedia: () -> Unit,
    onDuplicateSelectedMedia: () -> Unit,
    onAddSelectedMediaToCollection: (Collection) -> Unit,
) {
    val gridState = rememberLazyStaggeredGridState()
    val loadedImages = remember { mutableStateMapOf<String, Boolean>() }
    var startupComplete by remember { mutableStateOf(false) }
    val currentState by rememberUpdatedState(state)
    val reportStartupReady by rememberUpdatedState(onStartupReady)
    LaunchedEffect(Unit) {
        withTimeoutOrNull(3_000.milliseconds) {
            snapshotFlow {
                    val screen = currentState
                    val visible = gridState.layoutInfo.visibleItemsInfo
                    val ready =
                        !screen.isLoading &&
                            (screen.loadFailed || screen.mediaItems.isEmpty() ||
                                (visible.isNotEmpty() && visible.all { info ->
                                    val item = screen.mediaItems.getOrNull(info.index)
                                    item != null && item.id == info.key &&
                                        (item.image == null ||
                                            loadedImages[item.image.uri.toString()] == true)
                                }))

                    ready to visible.map { Triple(it.key, it.offset, it.size) }
                }
                .debounce(100.milliseconds)
                .first { it.first }
        }
        startupComplete = true
        loadedImages.clear()
        reportStartupReady()
    }

    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showMassDeletionConfirmation by rememberSaveable { mutableStateOf(false) }
    var collectionPendingDeletion by rememberSaveable { mutableStateOf<Collection?>(null) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .background(LocalAppColors.current.backgroundPrimary)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            focusManager.clearFocus()
                        }
                    )
                },
        topBar = {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(LocalAppColors.current.backgroundPrimary)
                        .padding(horizontal = 17.dp, vertical = 6.dp)
                        .statusBarsPadding()
                        .height(46.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(Modifier
                    .wrapContentHeight()
                    .padding(start = 8.dp)) {
                    Text(
                        text = stringResource(R.string.app_name_p1),
                        color = LocalAppColors.current.accentPrimary,
                        style = AppTypography.headlineMedium,
                    )
                    Text(
                        text = stringResource(R.string.app_name_p2),
                        color = LocalAppColors.current.accentSecondary,
                        style = AppTypography.headlineMedium,
                    )
                }

                Spacer(Modifier.weight(1f))

                if (state.isSelectionMode) {
                    Text(
                        modifier = Modifier.padding(end = 8.dp),
                        text =
                            pluralStringResource(
                                R.plurals.label_selected,
                                state.selectedMediaItemIds.size,
                                state.selectedMediaItemIds.size,
                            ),
                        style = AppTypography.bodyMedium,
                        color = LocalAppColors.current.contentPrimary,
                    )
                } else {
                    AppActionMenu<HiveCreateAction>(
                        onSelected = { action ->
                            when (action) {
                                HiveCreateAction.CREATE_MEDIA_ITEM -> onCreateMedia()
                                HiveCreateAction.CREATE_COLLECTION -> onCreateCollection()
                            }
                        }
                    ) { onClick ->
                        AppIconButton(
                            modifier = Modifier
                                .padding(7.dp)
                                .size(30.dp),
                            icon = painterResource(R.drawable.ic_add),
                        ) {
                            onClick()
                        }
                    }

                    AppActionMenu<HiveMenuAction>(
                        onSelected = { action ->
                            when (action) {
                                HiveMenuAction.ENABLE_PICKER -> onEnableNotification(true)

                                HiveMenuAction.VIEW_APP_INFO -> {
                                    showAbout = true
                                }

                                HiveMenuAction.IMPORT_COLLECTION -> {
                                    onCollectionImport()
                                }

                                HiveMenuAction.MASS_IMPORT -> {
                                    importImages()
                                }
                            }
                        }
                    ) { onClick ->
                        AppIconButton(
                            modifier = Modifier
                                .padding(7.dp)
                                .size(30.dp),
                            icon = painterResource(R.drawable.ic_more),
                        ) {
                            onClick()
                        }
                    }
                }
            }
        },
    ) { contentPadding ->
        var showSheet by rememberSaveable { mutableStateOf(false) }

        ConstraintLayout(
            modifier =
                Modifier
                    .padding(contentPadding)
                    .consumeWindowInsets(contentPadding)
                    .imePadding()
                    .background(LocalAppColors.current.backgroundPrimary)
                    .padding(horizontal = 10.dp)
                    .fillMaxSize()
        ) {
            val (content, actions, topBars) = createRefs()
            var topBarHeight by remember { mutableIntStateOf(0) }

            SearchAndCollectionBar(
                modifier =
                    Modifier
                        .zIndex(1f)
                        .constrainAs(topBars) {
                            top.linkTo(parent.top)
                        }
                        .onSizeChanged {
                            topBarHeight = it.height
                        },
                initialQuery = state.searchQuery,
                loadFailed = state.loadFailed,
                onRetry = onRetryLoading,
                onSearch = onSearchQueryChange,
                collections = state.collections,
                selectedCollectionId = state.selectedCollectionId,
                onCollectionSelected = onCollectionSelected,
                onAction = { collection, action ->
                    when (action) {
                        CollectionAction.EXPORT -> onExportCollection(collection)
                        CollectionAction.EDIT -> onEditCollection(collection)
                        CollectionAction.DELETE -> collectionPendingDeletion = collection
                    }
                },
            )

            val contentModifier =
                Modifier
                    .constrainAs(content) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                    .clip(MaterialTheme.shapes.medium)
                    .alpha(if (state.isLoading) 0.7f else 1f)

            if (state.mediaItems.isEmpty()) {
                if (state.searchQuery.isNotBlank()) EmptySearchResultMessage(contentModifier)
                else EmptyHiveMessage(contentModifier)
            } else
                LazyVerticalStaggeredGrid(
                    state = gridState,
                    modifier = contentModifier,
                    columns = StaggeredGridCells.Fixed(2),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding =
                        PaddingValues(
                            top =
                                with(LocalDensity.current) {
                                    topBarHeight.toDp() + 7.dp
                                }
                        ),
                ) {
                    items(state.mediaItems, key = { it.id }) { mediaItem ->
                        SelectableMediaItemCard(
                            mediaItem = mediaItem,
                            onImageLoaded = {
                                if (!startupComplete) {
                                    mediaItem.image?.let { loadedImages[it.uri.toString()] = true }
                                }
                            },
                            isSelected = mediaItem.id in state.selectedMediaItemIds,
                            onClick = {
                                if (state.isSelectionMode) onToggleMediaSelection(mediaItem.id)
                                else onOpenMedia(mediaItem.id)
                            },
                            onLongClick = {
                                if (!state.isSelectionMode) {
                                    onStartMediaSelection(mediaItem.id)
                                }
                            },
                        )
                    }
                }

            if (state.isSelectionMode)
                Column(
                    modifier =
                        Modifier
                            .constrainAs(actions) {
                                bottom.linkTo(parent.bottom)
                                end.linkTo(parent.end)
                            }
                            .padding(end = 20.dp, bottom = 40.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AppIconButton(
                        modifier =
                            Modifier
                                .size(30.dp)
                                .shadow(7.dp, MaterialTheme.shapes.small, clip = false)
                                .background(
                                    LocalAppColors.current.warning,
                                    MaterialTheme.shapes.small,
                                ),
                        shape = MaterialTheme.shapes.small,
                        icon = painterResource(R.drawable.ic_cancel_small),
                        iconSize = 25.dp,
                        tint = LocalAppColors.current.contentPrimary,
                    ) {
                        onCancelMediaSelection()
                    }

                    AppActionMenu<MediaSelectionAction>(
                        properties = PopupProperties(focusable = false),
                        onSelected = { action ->
                            when (action) {
                                MediaSelectionAction.ADD_TO_COLLECTION -> showSheet = true
                                MediaSelectionAction.DELETE -> {
                                    showMassDeletionConfirmation = true
                                }

                                MediaSelectionAction.DUPLICATE -> {
                                    onDuplicateSelectedMedia()
                                }
                            }
                        },
                    ) { onClick ->
                        AppIconButton(
                            modifier =
                                Modifier
                                    .size(50.dp)
                                    .shadow(9.dp, MaterialTheme.shapes.small, clip = false)
                                    .background(
                                        LocalAppColors.current.accentSecondary,
                                        MaterialTheme.shapes.small,
                                    ),
                            shape = MaterialTheme.shapes.small,
                            icon = painterResource(R.drawable.ic_edit),
                            iconSize = 28.dp,
                            tint = LocalAppColors.current.contentPrimary,
                        ) {
                            onClick()
                        }
                    }
                }
        }
        CollectionPickerSheet(
            show = showSheet,
            collections = state.collections,
            onDismissRequest = {
                showSheet = false
            },
            onCollectionSelected = { collection ->
                onAddSelectedMediaToCollection(collection)
                showSheet = false
            },
            onCreateCollection = { onCreateCollection() },
        )
    }
    if (showAbout) AboutAppDialog(onDismissRequest = { showAbout = false })
    CollectionEditorDialog(
        state = state.collectionEditor,
        onSubmit = onSubmitCollection,
        onDismissRequest = onDismissCollection,
    )

    if (showMassDeletionConfirmation)
        DeleteSelectedMediaItemsDialog(
            selectedCount = state.selectedMediaItemIds.size,
            onDismissRequest = { showMassDeletionConfirmation = false },
            onDelete = {
                showMassDeletionConfirmation = false
                onDeleteSelectedMedia()
            },
        )

    if (collectionPendingDeletion != null)
        DeleteCollectionDialog(
            collection = collectionPendingDeletion!!,
            onDismissRequest = { collectionPendingDeletion = null },
            onDelete = { deleteContents ->
                onDeleteCollection(collectionPendingDeletion!!, deleteContents)
                collectionPendingDeletion = null
            },
        )
}
