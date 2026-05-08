package com.farywave.memehive.ui.picker

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farywave.memehive.R
import com.farywave.memehive.ui.components.BasicCollectionsNavigation
import com.farywave.memehive.ui.components.MediaItemCardFull
import com.farywave.memehive.ui.components.SearchBar
import com.farywave.memehive.ui.model.MediaItem
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun PickerScreen(onItemClicked: (mediaItem: MediaItem) -> Unit) {
    val context = LocalContext.current
    val viewModel: PickerViewModel = viewModel(
        factory = PickerViewModelFactory(context)
    )
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
                        viewModel.onSearch()
                    }
                )
            }

            val collections by viewModel.collections.collectAsState()
            if (collections.size > 1) Box(Modifier.padding(horizontal = 10.dp)) {
                BasicCollectionsNavigation(
                    collections = collections,
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
                onItemClicked = {
                    Toast.makeText(
                        context,
                        context.getString(R.string.copied_to_clipboard),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}

@Composable
private fun Content(
    modifier: Modifier = Modifier,
    viewModel: PickerViewModel,
    onItemClicked: (mediaItem: MediaItem) -> Unit,
) {
    val mediaItems by viewModel.mediaItems.collectAsState()

    LazyVerticalStaggeredGrid(
        modifier = modifier,
        columns = StaggeredGridCells.Fixed(2),
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp),

        ) {
        items(mediaItems, key = { it.id }) { mediaItem ->
            MediaItemCardFull(
                mediaItem = mediaItem,
                onClick = {
                    onItemClicked(mediaItem)
                },
                onLongClick = { }
            )
        }
    }
}