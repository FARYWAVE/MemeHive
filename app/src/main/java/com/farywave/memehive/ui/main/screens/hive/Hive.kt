package com.farywave.memehive.ui.main.screens.hive

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.farywave.memehive.R
import com.farywave.memehive.ui.components.BasicIconButton
import com.farywave.memehive.ui.components.CollectionsNavigation
import com.farywave.memehive.ui.components.MediaItemCardFull
import com.farywave.memehive.ui.components.SearchBar
import com.farywave.memehive.ui.model.MediaItem
import com.farywave.memehive.ui.theme.LocalAppColors
import com.farywave.memehive.ui.theme.Typography

@Composable
fun Hive(viewModel: HiveViewModel) {
    val focusManager = LocalFocusManager.current
    Scaffold(modifier = Modifier
        .fillMaxSize()
        .background(LocalAppColors.current.backgroundPrimary)
        .clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) {
            focusManager.clearFocus()
        },
        topBar = { Toolbar() }
    ) { contentPadding ->
        Column(Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .background(LocalAppColors.current.backgroundPrimary),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
            Box(Modifier.padding(horizontal = 10.dp)) { SearchBar(viewModel::onSearch) }
            Box(Modifier.padding(horizontal = 10.dp)) { CollectionsNavigation(
                collections = viewModel.collections.collectAsState().value,
                selectedCollection = viewModel.selectedCollection.collectAsState().value,
                onCollectionSelected = viewModel::onCollectionSelected
            )}

            Content(modifier = Modifier.padding(horizontal = 10.dp), mediaItems = viewModel.mediaItems.collectAsState().value)
        }
    }
}

@Composable
private fun Toolbar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(LocalAppColors.current.backgroundPrimary)
            .padding(horizontal = 17.dp, vertical = 6.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier
            .wrapContentHeight()
            .padding(start = 8.dp),
        ){
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

        BasicIconButton(icon = painterResource(R.drawable.ic_search)) { }
        BasicIconButton(icon = painterResource(R.drawable.ic_add)) { }
        BasicIconButton(icon = painterResource(R.drawable.ic_menu)) { }
    }
}

@Composable
private fun Content(modifier: Modifier = Modifier, mediaItems: List<MediaItem>) {
    LazyVerticalStaggeredGrid(
        modifier = modifier,
        columns = StaggeredGridCells.Fixed(2),
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(mediaItems) { mediaItem ->
            MediaItemCardFull(mediaItem)
        }
    }
}
