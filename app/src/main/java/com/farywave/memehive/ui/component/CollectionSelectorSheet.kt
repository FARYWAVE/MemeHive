package com.farywave.memehive.ui.component

import android.widget.CheckBox
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.farywave.memehive.R
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.ui.simple_component.SimpleBottomSheet
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun CollectionSelectorSheet(
    show: Boolean,
    selectedIds: Set<Long>,
    collections: List<Collection>,
    onDismiss: () -> Unit,
    onCollectionClicked: (Collection) -> Unit,
    onNewCollectionClicked: () -> Unit
) {
    var originallySelectedIds by remember {
        mutableStateOf<Set<Long>?>(null)
    }

    LaunchedEffect(Unit) {
        originallySelectedIds = selectedIds
    }

    val searchQuery = remember { mutableStateOf("") }
    val filteredCollections by remember(collections, searchQuery.value) {
        derivedStateOf {
            collections.filter {
                it.name.contains(searchQuery.value, ignoreCase = true) &&
                        originallySelectedIds?.contains(it.id)?.not() ?: false
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

            collections.filter { originallySelectedIds?.contains(it.id) ?: true }
                .forEach { collection ->
                    item {
                        CollectionItem(
                            collection = collection,
                            isSelected = selectedIds.contains(collection.id)
                        ) { onCollectionClicked(collection) }
                    }
                }

            item {
                SearchBar(hint = stringResource(R.string.hint_collection_search)) {
                    searchQuery.value = it
                }
            }

            filteredCollections.forEach { collection ->
                item {
                    CollectionItem(
                        collection = collection,
                        isSelected = selectedIds.contains(collection.id)
                    ) { onCollectionClicked(collection) }
                }
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
            text = stringResource(R.string.title_collections),
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
private fun CollectionItem(collection: Collection, isSelected: Boolean, onClicked: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    onClicked()
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        var loadFailed by remember(collection.cover?.uri) { mutableStateOf(false) }
        if (collection.cover != null && !loadFailed) AsyncImage(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop,
            model = collection.cover.uri,
            onError = { loadFailed = true },
            contentDescription = null
        ) else Icon(
            modifier = Modifier.size(40.dp),
            painter = painterResource(id = if (loadFailed) R.drawable.ic_error else R.drawable.ic_no_image),
            contentDescription = stringResource(if (loadFailed) R.string.error_image_unavailable else R.string.label_no_image),
            tint = LocalAppColors.current.contentSecondary
        )
        Column(
            modifier = Modifier
                .wrapContentSize(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = collection.name,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAppColors.current.contentPrimary
            )
            Text(
                text = pluralStringResource(R.plurals.collection_item_count, collection.mediaItemCount, collection.mediaItemCount),
                style = MaterialTheme.typography.labelMedium,
                color = LocalAppColors.current.contentSecondary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        CheckBox(checked = isSelected, onClick = {})
    }
}