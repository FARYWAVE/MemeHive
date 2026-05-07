package com.farywave.memehive.ui.main.screens.hive

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farywave.memehive.R
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.ui.components.CollectionPickerSheet
import com.farywave.memehive.ui.components.CollectionsNavigation
import com.farywave.memehive.ui.components.MediaItemCardFull
import com.farywave.memehive.ui.components.SearchBar
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.MediaItem
import com.farywave.memehive.ui.navigation.NavEvent
import com.farywave.memehive.ui.simple_components.SimpleActionMenu
import com.farywave.memehive.ui.simple_components.SimpleIconButton
import com.farywave.memehive.ui.theme.LocalAppColors
import com.farywave.memehive.ui.theme.Typography
import kotlinx.coroutines.flow.combine

@Composable
fun Hive(
    savedStateHandle: SavedStateHandle,
    onNavigate: (NavEvent) -> Unit
) {
    val context = LocalContext.current
    val viewModel: HiveViewModel = viewModel(
        factory = HiveViewModelFactory(context)
    )
    viewModel.loadCollections()
    val focusManager = LocalFocusManager.current

    val newCollectionEvent = remember {
        savedStateHandle.getStateFlow<String?>(
            "newCollectionName",
            null
        )
    }

    val renamedCollectionEvent = remember {
        combine(
            savedStateHandle.getStateFlow<String?>(
                "renamedCollectionName",
                null
            ),
            savedStateHandle.getStateFlow<Long?>(
                "renamedCollectionId",
                null
            )
        ) { name, id ->

            if (!name.isNullOrBlank() && id != null) {
                id to name
            } else {
                null
            }
        }
    }


    LaunchedEffect(newCollectionEvent) {
        newCollectionEvent.collect { name ->

            if (!name.isNullOrBlank()) {
                viewModel.createCollection(name)

                savedStateHandle["newCollectionName"] = null
            }
        }
    }

    LaunchedEffect(renamedCollectionEvent) {
        renamedCollectionEvent.collect { event ->

            event?.let { (id, name) ->

                viewModel.renameCollection(id, name)

                savedStateHandle["renamedCollectionName"] = null
                savedStateHandle["renamedCollectionId"] = null
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.backgroundPrimary)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
            },
        topBar = {
            Toolbar(
                onMassImport = { uris -> viewModel.onMassImport(context, uris) },
                onNavigate = onNavigate
            )
        }
    ) { contentPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(LocalAppColors.current.backgroundPrimary),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(Modifier.padding(horizontal = 10.dp)) {
                SearchBar(
                    hint = stringResource(R.string.media_search_hint),
                    onQueryChange = {
                        viewModel.onSearchQueryChanged(it)
                        viewModel.onSearch()
                    }
                )
            }
            var selectedCollectionForCover by remember { mutableStateOf<Collection?>(null) }
            val launcher = DeviceTools.requestMedia { uri ->
                val collection = selectedCollectionForCover
                if (uri != null && collection != null) {
                    viewModel.setCollectionCover(context, collection, uri)
                }
            }

            val collections by viewModel.collections.collectAsState()
            if (collections.size > 1) Box(Modifier.padding(horizontal = 10.dp)) {
                CollectionsNavigation(
                    collections = collections,
                    selectedCollection = viewModel.selectedCollection.collectAsState().value,
                    onCollectionSelected = {
                        viewModel.onCollectionSelected(it)
                        viewModel.onSearch()
                    },
                    onAction = { collection, action ->
                        when (action) {
                            CollectionActions.RENAME -> {
                                onNavigate(
                                    NavEvent.ToRenameCollectionDialog(
                                        collection.name,
                                        collection.id
                                    )
                                )
                            }

                            CollectionActions.SET_COVER -> {
                                selectedCollectionForCover = collection
                                launcher.launch("image/*")
                            }

                            CollectionActions.DELETE -> {
                                viewModel.deleteCollection(collection)
                            }
                        }
                    }
                )
            }

            Content(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxSize(),
                viewModel = viewModel,
                onNavigate = onNavigate
            )
        }
    }
}

@Composable
private fun Toolbar(onMassImport: (uris: List<Uri>) -> Unit, onNavigate: (NavEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(LocalAppColors.current.backgroundPrimary)
            .padding(horizontal = 17.dp, vertical = 6.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val launcher = DeviceTools.requestMultipleMedia { onMassImport(it) }

        Row(
            Modifier
                .wrapContentHeight()
                .padding(start = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name_p1),
                color = LocalAppColors.current.accentPrimary,
                style = Typography.headlineMedium
            )
            Text(
                text = stringResource(R.string.app_name_p2),
                color = LocalAppColors.current.accentSecondary,
                style = Typography.headlineMedium
            )
        }

        Spacer(Modifier.weight(1f))

        SimpleIconButton(
            modifier = Modifier
                .padding(7.dp)
                .size(30.dp),
            icon = painterResource(R.drawable.ic_search)
        ) {}

        SimpleActionMenu<CreateActions>(onSelected = { action ->
            when (action) {
                CreateActions.CREATE_MEDIA_ITEM -> onNavigate(NavEvent.ToEditing(-1))
                CreateActions.CREATE_COLLECTION -> onNavigate(NavEvent.NewCollectionDialog)
            }
        }) { onClick ->
            SimpleIconButton(
                modifier = Modifier
                    .padding(7.dp)
                    .size(30.dp),
                icon = painterResource(R.drawable.ic_add)
            ) {
                onClick()
            }
        }

        SimpleActionMenu<MoreActions>(onSelected = { action ->
            when (action) {
                MoreActions.VIEW_APP_INFO -> onNavigate(NavEvent.AboutAppDialog)
                MoreActions.IMPORT_COLLECTION -> {}
                MoreActions.MASS_IMPORT -> {
                    launcher.launch("image/*")
                }
            }
        }) { onClick ->
            SimpleIconButton(
                modifier = Modifier
                    .padding(7.dp)
                    .size(30.dp),
                icon = painterResource(R.drawable.ic_more)
            ) {
                onClick()
            }
        }
    }
}

@Composable
private fun Content(
    modifier: Modifier = Modifier,
    viewModel: HiveViewModel,
    onNavigate: (NavEvent) -> Unit
) {
    val mediaItems by viewModel.mediaItems.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val isMassEditingMode by viewModel.isMassEditingMode.collectAsState()
    var showSheet by remember { mutableStateOf(false) }


    ConstraintLayout(modifier = modifier) {
        val (content, actions) = createRefs()
        LazyVerticalStaggeredGrid(
            modifier = Modifier.constrainAs(content) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            },
            columns = StaggeredGridCells.Fixed(2),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),

            ) {
            items(mediaItems, key = { it.id }) { mediaItem ->
                MediaItemCardFull(
                    mediaItem = mediaItem,
                    onClick = {
                        if (isMassEditingMode) viewModel.toggleItemSelection(mediaItem)
                        else onNavigate(NavEvent.ToEditing(mediaItem.id))
                    },
                    onLongClick = {
                        if (!isMassEditingMode) {
                            viewModel.enableMassEditingMode()
                            viewModel.selectItem(mediaItem)
                        }
                    }
                )
            }
        }

        if (viewModel.isMassEditingMode.collectAsState().value) Column(
            modifier = Modifier
                .constrainAs(actions) {
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                }
                .padding(end = 20.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            SimpleIconButton(
                modifier = Modifier
                    .size(30.dp)
                    .shadow(7.dp, RoundedCornerShape(10.dp), clip = false)
                    .background(
                        LocalAppColors.current.warning,
                        RoundedCornerShape(10.dp)
                    ),
                icon = painterResource(R.drawable.ic_cancel_small),
                iconSize = 25.dp,
                tint = LocalAppColors.current.contentPrimary
            ) { viewModel.disableMassEditingMode() }

            SimpleActionMenu<MassEditActions>(
                onSelected = { action ->
                    when (action) {
                        MassEditActions.ADD_TO_COLLECTION -> showSheet = true
                        MassEditActions.DELETE -> {
                            viewModel.deleteSelectedMediaItems()
                            viewModel.disableMassEditingMode()
                        }

                        MassEditActions.DUPLICATE -> {
                            viewModel.duplicateSelectedMediaItems()
                            viewModel.disableMassEditingMode()
                        }
                    }

                }
            ) { onClick ->
                SimpleIconButton(
                    modifier = Modifier
                        .size(50.dp)
                        .shadow(9.dp, MaterialTheme.shapes.small, clip = false)
                        .background(
                            LocalAppColors.current.accentSecondary,
                            MaterialTheme.shapes.small
                        ),
                    icon = painterResource(R.drawable.ic_edit),
                    iconSize = 28.dp,
                    tint = LocalAppColors.current.contentPrimary
                ) { onClick() }
            }
        }
    }
    CollectionPickerSheet(
        show = showSheet,
        collections = collections,
        onDismiss = {
            showSheet = false
        },
        onCollectionSelected = { collection ->
            viewModel.addSelectedMediaItemsToCollection(collection)
            showSheet = false
            viewModel.disableMassEditingMode()
        },
        onNewCollectionClicked = { onNavigate(NavEvent.NewCollectionDialog) }
    )
}