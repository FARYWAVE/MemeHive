package com.farywave.memehive.ui.component

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.farywave.memehive.R
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun ImageWithPlaceholder(
    modifier: Modifier,
    shape: Shape,
    imageUri: Uri?,
    retryKey: Int = 0,
    onLoadFailed: () -> Unit = {},
) {
    var loadFailed by remember(imageUri, retryKey) { mutableStateOf(false) }

    if (imageUri != null && !loadFailed)
        AsyncImage(
            model = imageUri,
            onError = {
                loadFailed = true
                onLoadFailed()
            },
            contentDescription = null,
            modifier = modifier.fillMaxWidth().clip(shape),
            contentScale = ContentScale.FillWidth,
        )
    else
        Box(
            modifier =
                modifier
                    .aspectRatio(1.5F)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(color = LocalAppColors.current.backgroundPrimary)
                    .border(
                        3.dp,
                        LocalAppColors.current.backgroundSecondary,
                        shape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(40.dp),
                painter =
                    painterResource(
                        id = if (loadFailed) R.drawable.ic_error else R.drawable.ic_no_image
                    ),
                tint = LocalAppColors.current.contentSecondary,
                contentDescription =
                    stringResource(
                        if (loadFailed) R.string.error_image_unavailable
                        else R.string.label_no_image
                    ),
            )
        }
}
