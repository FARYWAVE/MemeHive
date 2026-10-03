package com.farywave.memehive.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import com.farywave.memehive.ui.activity.main.screen.hive.CollectionActions
import com.farywave.memehive.ui.model.Collection
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

    if (entries.size > 4) {
        LazyRow(
            modifier = barModifier,
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
        Row(
            modifier = barModifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            entries.forEach { collection ->
                key(collection?.id) {
                    CollectionNavigationItem(
                        modifier = Modifier.weight(1f),
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
            text = collection?.name ?: "All",
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
            .background(
                if (isSelected) LocalAppColors.current.accentPrimary
                else LocalAppColors.current.transparent,
                shape = MaterialTheme.shapes.large
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = LocalAppColors.current.accentPrimary
                ),
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) LocalAppColors.current.contentPrimary else LocalAppColors.current.contentSecondary,
        )
    }
}
