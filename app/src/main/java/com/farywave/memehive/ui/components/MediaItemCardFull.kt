package com.farywave.memehive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.AsyncImage
import com.farywave.memehive.R
import com.farywave.memehive.ui.model.MediaItem
import com.farywave.memehive.ui.theme.LocalAppColors
import java.io.File


@Composable
fun MediaItemCardFull(
    mediaItem: MediaItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focusManager = LocalFocusManager.current
    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                LocalAppColors.current.backgroundSecondary,
                MaterialTheme.shapes.medium
            )
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    focusManager.clearFocus()
                    onClick()
                },
                onLongClick = {
                    focusManager.clearFocus()
                    onLongClick()
                },
            )
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            val (image, selection, name) = createRefs()

            NullableImage(
                modifier = Modifier
                    .constrainAs(image) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .wrapContentSize(),
                src = mediaItem.src,
                roundBottom = mediaItem.tags.isEmpty()
            )

            if (mediaItem.isSelected) Selection(
                modifier = Modifier.constrainAs(selection) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                }
            )

            if (mediaItem.caption?.isNotEmpty() == true) Name(
                modifier = Modifier.constrainAs(name) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                },
                name = mediaItem.caption.orEmpty()
            )
        }

        if (mediaItem.tags.isNotEmpty()) FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 7.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            mediaItem.tags.forEach { tag ->
                TagChip(tag)
            }
        }
    }
}

@Composable
private fun NullableImage(modifier: Modifier, src: File?, roundBottom: Boolean) {
    val roundedTopShape = MaterialTheme.shapes.medium.copy(
        bottomStart = CornerSize(0.dp),
        bottomEnd = CornerSize(0.dp)
    )
    if (src != null) AsyncImage(
        model = src,
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth()
            .clip(roundedTopShape),
        contentScale = ContentScale.FillWidth
    ) else Box(
        modifier = modifier
            .aspectRatio(1.5F)
            .fillMaxWidth()
            .background(
                color = LocalAppColors.current.backgroundPrimary,
                shape = roundedTopShape
            )
            .border(
                3.dp,
                LocalAppColors.current.backgroundSecondary,
                if (roundBottom) MaterialTheme.shapes.medium
                else roundedTopShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(40.dp),
            painter = painterResource(id = R.drawable.ic_no_image),
            tint = LocalAppColors.current.contentSecondary,
            contentDescription = null
        )
    }
}

@Composable
private fun Selection(modifier: Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val size = textMeasurer.measure(
        text = AnnotatedString("Ambatukam"),
        style = MaterialTheme.typography.bodyMedium
    ).size.height
    Box(
        modifier = modifier
            .background(
                color = LocalAppColors.current.tint,
                shape = MaterialTheme.shapes.small.copy(
                    topStart = CornerSize(0.dp),
                    topEnd = CornerSize(0.dp),
                    bottomEnd = CornerSize(0.dp)
                )
            )
            .wrapContentSize()
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .size(with(LocalDensity.current) { size.toDp() }),
            painter = painterResource(id = R.drawable.ic_check),
            tint = LocalAppColors.current.accentSecondary,
            contentDescription = null
        )
    }
}

@Composable
private fun Name(modifier: Modifier, name: String) {
    Box(
        modifier = modifier
            .background(
                color = LocalAppColors.current.tint,
                shape = MaterialTheme.shapes.small.copy(
                    topStart = CornerSize(0.dp),
                    bottomStart = CornerSize(0.dp),
                    bottomEnd = CornerSize(0.dp)
                )
            )
            .wrapContentSize()
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalAppColors.current.contentPrimary
        )
    }
}

@Composable
private fun TagChip(tag: String) {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .background(
                LocalAppColors.current.accentPrimary,
                shape = MaterialTheme.shapes.large
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
            text = tag,
            style = MaterialTheme.typography.labelMedium,
            color = LocalAppColors.current.contentPrimary
        )
    }
}
