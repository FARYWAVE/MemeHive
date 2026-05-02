package com.farywave.memehive.ui.main.screens.hive

import android.util.Log
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farywave.memehive.R
import com.farywave.memehive.ui.components.ActionMenuOptions
import com.farywave.memehive.ui.components.CollectionsNavigation
import com.farywave.memehive.ui.components.MediaItemCardFull
import com.farywave.memehive.ui.components.SearchBar
import com.farywave.memehive.ui.components.SimpleActionMenu
import com.farywave.memehive.ui.components.SimpleIconButton
import com.farywave.memehive.ui.navigation.NavEvent
import com.farywave.memehive.ui.theme.LocalAppColors
import com.farywave.memehive.ui.theme.MemeHiveTheme
import com.farywave.memehive.ui.theme.Typography

@Composable
fun Hive(onNavigate: (NavEvent) -> Unit) {
    val context = LocalContext.current
    val viewModel: HiveViewModel = viewModel(
        factory = HiveViewModelFactory(context)
    )
    viewModel.loadCollections()
    val focusManager = LocalFocusManager.current
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
        topBar = { Toolbar(onNavigate) }
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
                    stringResource(R.string.media_search_hint),
                    {
                        viewModel.onSearchQueryChanged(it)
                        viewModel.onSearch()
                    }
                )
            }
            Box(Modifier.padding(horizontal = 10.dp)) {
                CollectionsNavigation(
                    collections = viewModel.collections.collectAsState().value,
                    selectedCollection = viewModel.selectedCollection.collectAsState().value,
                    onCollectionSelected = {
                        viewModel.onCollectionSelected(it)
                        viewModel.onSearch()
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
private fun Toolbar(onNavigate: (NavEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(LocalAppColors.current.backgroundPrimary)
            .padding(horizontal = 17.dp, vertical = 6.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                CreateActions.CREATE_COLLECTION -> {}
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
            Log.d("TEST", action.toString())
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
    val isMassEditingMode by viewModel.isMassEditingMode.collectAsState()
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
}

@Preview
@Composable
private fun Preview() {
    MemeHiveTheme {
        Hive({})
    }
}
