package com.farywave.memehive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.farywave.memehive.ui.main.screens.hive.CollectionActions
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.simple_components.SimpleActionMenu
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun CollectionsNavigation(
    modifier: Modifier = Modifier,
    collections: List<Collection>,
    selectedCollection: Collection,
    onCollectionSelected: (Collection) -> Unit,
    onAction: (collection: Collection, action: CollectionActions) -> Unit
) {
    if (collections.size > 4) LazyRow(
        modifier = modifier
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
            .padding(horizontal = 5.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(collections) { collection ->
            if (collection.id == -1L) CollectionChip(
                modifier = Modifier.wrapContentSize(),
                collection = collection,
                isSelected = collection.id == selectedCollection.id,
                onClick = { onCollectionSelected(collection) },
                onLongClick = { })
            else SimpleActionMenu<CollectionActions>(
                onSelected = { action -> onAction(collection, action) },
            ) { onClick ->
                CollectionChip(
                    modifier = Modifier.wrapContentSize(),
                    collection = collection,
                    isSelected = collection.id == selectedCollection.id,
                    onClick = { onCollectionSelected(collection) },
                    onLongClick = { onClick() })
            }
        }
    } else Row(
        modifier = modifier
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
            .padding(horizontal = 5.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        collections.forEach { collection ->
            Box(
                modifier = Modifier.weight(1f)
            ) {
                if (collection.id == -1L) CollectionChip(
                    modifier = Modifier.fillMaxWidth(),
                    collection = collection,
                    isSelected = collection.id == selectedCollection.id,
                    onClick = { onCollectionSelected(collection) },
                    onLongClick = { }
                ) else SimpleActionMenu<CollectionActions>(
                    onSelected = { action ->
                        onAction(collection, action)
                    },
                ) { onClick ->

                    CollectionChip(
                        modifier = Modifier.fillMaxWidth(),
                        collection = collection,
                        isSelected = collection.id == selectedCollection.id,
                        onClick = { onCollectionSelected(collection) },
                        onLongClick = { onClick() }
                    )
                }
            }
        }
    }
}

@Composable
fun BasicCollectionsNavigation(
    modifier: Modifier = Modifier,
    collections: List<Collection>,
    selectedCollection: Collection,
    onCollectionSelected: (Collection) -> Unit
) {
    if (collections.size > 4) LazyRow(
        modifier = modifier
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
            .padding(horizontal = 5.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(collections) { collection ->
            CollectionChip(
                modifier = Modifier.wrapContentSize(),
                collection = collection,
                isSelected = collection.id == selectedCollection.id,
                onClick = { onCollectionSelected(collection) },
                onLongClick = { }
            )
        }
    } else Row(
        modifier = modifier
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
            .padding(horizontal = 5.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        collections.forEach { collection ->
            Box(
                modifier = Modifier.weight(1f)
            ) {
                CollectionChip(
                    modifier = Modifier.fillMaxWidth(),
                    collection = collection,
                    isSelected = collection.id == selectedCollection.id,
                    onClick = { onCollectionSelected(collection) },
                    onLongClick = { }
                )
            }
        }
    }
}

@Composable
private fun CollectionChip(
    modifier: Modifier,
    collection: Collection,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
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
            text = collection.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) LocalAppColors.current.contentPrimary else LocalAppColors.current.contentSecondary,
        )
    }
}