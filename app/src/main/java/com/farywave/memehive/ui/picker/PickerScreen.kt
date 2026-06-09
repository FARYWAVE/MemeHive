package com.farywave.memehive.ui.picker

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farywave.memehive.R
import com.farywave.memehive.ui.components.MediaItemCardSimple
import com.farywave.memehive.ui.components.SearchAndCollectionBar
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
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        focusManager.clearFocus()
                    }
                )
            }
    ) { contentPadding ->
        Content(
            modifier = Modifier
                .padding(contentPadding)
                .padding(horizontal = 10.dp)
                .padding(top = 7.dp)
                .background(LocalAppColors.current.backgroundPrimary)
                .fillMaxSize(),
            viewModel = viewModel,
            onItemClicked = {
                Toast.makeText(
                    context,
                    context.getString(R.string.copied_to_clipboard),
                    Toast.LENGTH_SHORT
                ).show()
                onItemClicked(it)
            }
        )
    }
}

@Composable
private fun Content(
    modifier: Modifier = Modifier,
    viewModel: PickerViewModel,
    onItemClicked: (mediaItem: MediaItem) -> Unit,
) {
    val mediaItems by viewModel.mediaItems.collectAsState()
    Box(Modifier.background(LocalAppColors.current.backgroundPrimary)) {
        ConstraintLayout(
            modifier = modifier
        ) {
            val (content, topBars) = createRefs()
            var topBarHeight by remember { mutableStateOf(0) }

            SearchAndCollectionBar(
                modifier = Modifier
                    .zIndex(1f)
                    .constrainAs(topBars) {
                        top.linkTo(parent.top)
                    }
                    .onSizeChanged {
                        topBarHeight = it.height
                    },
                onSearch = {
                    viewModel.onSearchQueryChanged(it)
                    viewModel.onSearch()
                },
                simpleCollectionBar = true,
                collections = viewModel.collections.collectAsState().value,
                selectedCollection = viewModel.selectedCollection.collectAsState().value,
                onCollectionSelected = {
                    viewModel.onCollectionSelected(it)
                    viewModel.onSearch()
                },
                onAction = { _, _ -> }
            )

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
                contentPadding = PaddingValues(
                    top = with(LocalDensity.current) {
                        topBarHeight.toDp() + 7.dp
                    }
                )
            ) {
                items(mediaItems, key = { it.id }) { mediaItem ->
                    MediaItemCardSimple(mediaItem) { onItemClicked(mediaItem) }
                }
            }
        }
    }
}