package com.farywave.memehive.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.farywave.memehive.R
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.ui.activity.main.screen.hive.CollectionActions
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun SearchAndCollectionBar(
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit,
    initialQuery: String = "",
    collections: List<Collection>,
    selectedCollectionId: Long?,
    onCollectionSelected: (Long?) -> Unit,
    onAction: ((collection: Collection, action: CollectionActions) -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        LocalAppColors.current.backgroundPrimary,
                        LocalAppColors.current.backgroundPrimary.copy(alpha = 0F)
                    )
                )
            ),
        verticalArrangement = Arrangement.spacedBy(7.dp)

    ) {
        SearchBar(
            modifier = Modifier.padding(horizontal = 3.dp),
            hint = stringResource(R.string.media_search_hint),
            initialQuery = initialQuery,
            onQueryChange = onSearch
        )

        if (collections.isNotEmpty()) {
            CollectionNavigation(
                modifier = Modifier.padding(horizontal = 3.dp),
                collections = collections,
                selectedCollectionId = selectedCollectionId,
                onCollectionSelected = onCollectionSelected,
                onAction = onAction
            )
        }
    }
}
