package com.farywave.memehive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun CollectionsNavigation(modifier: Modifier = Modifier, collections: List<Collection>, selectedCollection: Collection, onCollectionSelected: (Collection) -> Unit) {
    LazyRow(
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
            CollectionChip(collection, collection == selectedCollection) {
                onCollectionSelected(collection)
            }
        }
    }
}

@Composable
private fun CollectionChip(collection: Collection, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .wrapContentSize()
            .background(
                if (isSelected) LocalAppColors.current.accentPrimary
                else LocalAppColors.current.transparent,
                shape = MaterialTheme.shapes.large
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = LocalAppColors.current.accentPrimary
                ),
                onClick = onClick
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