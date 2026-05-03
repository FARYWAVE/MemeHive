package com.farywave.memehive.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.farywave.memehive.R
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.theme.LocalAppColors


@Composable
fun SingleCollectionPicker(
    show: Boolean,
    collections: List<Collection>,
    onDismiss: () -> Unit,
    onCollectionSelected: (Collection) -> Unit,
    onNewCollectionClicked: () -> Unit
) {
    val searchQuery = remember { mutableStateOf("") }
    val filteredCollections by remember(collections, searchQuery.value) {
        derivedStateOf {
            collections.filter {
                it.name.contains(searchQuery.value, ignoreCase = true)
            }
        }
    }

    SimpleBottomSheet(show, onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(vertical = 10.dp, horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Heading { onNewCollectionClicked() } }

            item {
                SearchBar(hint = stringResource(R.string.collection_search_hint)) {
                    searchQuery.value = it
                }
            }

            items(filteredCollections) { collection ->
                if (collection.id != -1L) CollectionItem(collection) { onCollectionSelected(collection) }
            }
        }
    }
}

@Composable
private fun Heading(onNewCollectionClicked: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight(), verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = stringResource(R.string.header_collections),
            style = MaterialTheme.typography.titleMedium,
            color = LocalAppColors.current.contentPrimary
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            modifier = Modifier.clickable(
                onClick = { onNewCollectionClicked() }
            ),
            text = stringResource(R.string.action_create_collection),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalAppColors.current.accentPrimary
        )
    }
}

@Composable
private fun CollectionItem(collection: Collection, onClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable(onClick = onClicked),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (collection.cover != null) AsyncImage(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.small),
            model = collection.cover,
            contentDescription = null
        ) else Icon(
            modifier = Modifier.size(40.dp),
            painter = painterResource(id = R.drawable.ic_no_image),
            contentDescription = null,
            tint = LocalAppColors.current.contentSecondary
        )
        Text(
            text = collection.name,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalAppColors.current.contentPrimary
        )
        Spacer(modifier = Modifier.weight(1f))
        val itemCountText = stringResource(R.string.collection_items_count)
        Text(
            text = "${collection.mediaItemCount} $itemCountText",
            style = MaterialTheme.typography.labelMedium,
            color = LocalAppColors.current.contentSecondary
        )
    }
}