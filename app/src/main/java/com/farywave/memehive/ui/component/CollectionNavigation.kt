package com.farywave.memehive.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.farywave.memehive.R
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.ui.activity.main.screen.hive.CollectionActions
import com.farywave.memehive.ui.simple_component.SimpleActionMenu
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun CollectionNavigation(
    modifier: Modifier = Modifier,
    collections: List<Collection>,
    selectedCollectionId: Long?,
    onCollectionSelected: (Long?) -> Unit,
    onAction: ((collection: Collection, action: CollectionActions) -> Unit)? = null
) {
    val entries = listOf<Collection?>(null) + collections
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val barModifier = modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .background(
            LocalAppColors.current.backgroundSecondary,
            shape = MaterialTheme.shapes.large
        )
        .border(
            width = 4.dp,
            color = LocalAppColors.current.backgroundSecondary,
            shape = MaterialTheme.shapes.large
        )
        .clip(MaterialTheme.shapes.large)
        .padding(horizontal = 5.dp, vertical = 4.dp)

    BoxWithConstraints(modifier = barModifier) {
        val chipWidths = entries.map { collection ->
            textMeasurer.measure(
                text = collection?.name ?: stringResource(R.string.label_all_collections),
                style = MaterialTheme.typography.bodyMedium,
                softWrap = false,
                maxLines = 1
            ).size.width + with(density) { CollectionChipHorizontalPadding.roundToPx() * 2 }
        }
        val naturalWidth = chipWidths.sum()

        if (naturalWidth > constraints.maxWidth) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(entries, key = { it?.id ?: "all" }) { collection ->
                    CollectionNavigationItem(
                        collection = collection,
                        selectedCollectionId = selectedCollectionId,
                        onCollectionSelected = onCollectionSelected,
                        onAction = onAction
                    )
                }
            }
        } else {
            val extraWidthPerChip = (constraints.maxWidth - naturalWidth).toFloat() / entries.size
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                entries.forEachIndexed { index, collection ->
                    key(collection?.id) {
                        CollectionNavigationItem(
                            modifier = Modifier.weight(chipWidths[index] + extraWidthPerChip),
                            collection = collection,
                            selectedCollectionId = selectedCollectionId,
                            onCollectionSelected = onCollectionSelected,
                            onAction = onAction
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionNavigationItem(
    modifier: Modifier = Modifier,
    collection: Collection?,
    selectedCollectionId: Long?,
    onCollectionSelected: (Long?) -> Unit,
    onAction: ((Collection, CollectionActions) -> Unit)?
) {
    val chip: @Composable (onLongClick: (() -> Unit)?) -> Unit = { onLongClick ->
        CollectionChip(
            modifier = Modifier.fillMaxWidth(),
            text = collection?.name ?: stringResource(R.string.label_all_collections),
            isSelected = collection?.id == selectedCollectionId,
            onClick = { onCollectionSelected(collection?.id) },
            onLongClick = onLongClick
        )
    }

    Box(modifier = modifier) {
        if (collection != null && onAction != null) {
            SimpleActionMenu<CollectionActions>(
                onSelected = { action -> onAction(collection, action) }
            ) { onLongClick ->
                chip(onLongClick)
            }
        } else {
            chip(null)
        }
    }
}

private val CollectionChipHorizontalPadding = 10.dp

@Composable
private fun CollectionChip(
    modifier: Modifier,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(
                if (isSelected) LocalAppColors.current.accentPrimary
                else LocalAppColors.current.transparent,
                shape = MaterialTheme.shapes.large
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = LocalAppColors.current.accentPrimary
                ),
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = CollectionChipHorizontalPadding, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) LocalAppColors.current.contentPrimary else LocalAppColors.current.contentSecondary,
        )
    }
}
