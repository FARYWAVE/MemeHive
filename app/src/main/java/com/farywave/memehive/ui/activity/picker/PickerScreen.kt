package com.farywave.memehive.ui.activity.picker

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farywave.memehive.MemeHiveApplication
import com.farywave.memehive.R
import com.farywave.memehive.ui.component.MediaItemCardSimple
import com.farywave.memehive.ui.component.SearchAndCollectionBar
import com.farywave.memehive.domain.model.MediaItem
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun PickerScreen(onItemClicked: (mediaItem: MediaItem, onFailure: () -> Unit) -> Unit) {
    val context = LocalContext.current
    val viewModel: PickerViewModel =
        (context.applicationContext as MemeHiveApplication).container.let {
            viewModel(
                factory = PickerViewModelFactory(it.collectionUseCases, it.mediaSearchUseCase)
            )
        }

    val error by viewModel.error.collectAsStateWithLifecycle()
    LaunchedEffect(error?.id) {
        val currentError = error ?: return@LaunchedEffect
        // TODO: Display currentError.messageResId to the user before acknowledging it.
        viewModel.clearError(currentError.id)
    }

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
                onItemClicked(it, viewModel::reportCopyFailure)
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
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val mediaItems by viewModel.mediaItems.collectAsStateWithLifecycle()
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
                initialQuery = searchQuery,
                onSearch = viewModel::onSearchQueryChanged,
                collections = viewModel.collections.collectAsStateWithLifecycle().value,
                selectedCollectionId = viewModel.selectedCollectionId.collectAsStateWithLifecycle().value,
                onCollectionSelected = viewModel::onCollectionSelected
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
