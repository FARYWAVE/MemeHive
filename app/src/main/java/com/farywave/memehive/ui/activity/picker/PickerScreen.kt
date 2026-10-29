package com.farywave.memehive.ui.activity.picker

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.farywave.memehive.domain.model.MediaItem
import com.farywave.memehive.ui.component.MediaItemCardSimple
import com.farywave.memehive.ui.component.SearchAndCollectionBar
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun PickerScreen(
    state: PickerUiState,
    onSearchQueryChanged: (String) -> Unit,
    onRetryLoading: () -> Unit = {},
    onCollectionSelected: (Long?) -> Unit,
    onItemClicked: (MediaItem) -> Unit,
) {
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
        Box(Modifier.background(LocalAppColors.current.backgroundPrimary)) {
            ConstraintLayout(
                modifier = Modifier
                    .padding(contentPadding)
                    .padding(horizontal = 10.dp)
                    .padding(top = 7.dp)
                    .background(LocalAppColors.current.backgroundPrimary)
                    .fillMaxSize()
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
                    initialQuery = state.searchQuery,
                    isLoading = state.isLoading,
                    loadFailed = state.loadFailed,
                    onRetry = onRetryLoading,
                    onSearch = onSearchQueryChanged,
                    collections = state.collections,
                    selectedCollectionId = state.selectedCollectionId,
                    onCollectionSelected = onCollectionSelected
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
                    items(state.mediaItems, key = { it.id }) { mediaItem ->
                        MediaItemCardSimple(mediaItem) { onItemClicked(mediaItem) }
                    }
                }
            }
        }
    }
}
