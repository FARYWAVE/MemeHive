package com.farywave.memehive.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.farywave.memehive.R
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun ImagePicker(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    initialSrc: Uri?,
    onImageSelected: (Uri?) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val launcher = DeviceTools.requestMedia { onImageSelected(it) }
    NullableImage(
        modifier
            .fillMaxWidth()
            .clickable(onClick = {
                focusManager.clearFocus()
                launcher.launch("image/*")
            }),
        shape,
        initialSrc
    )
}

@Composable
private fun NullableImage(modifier: Modifier, shape: RoundedCornerShape, src: Uri?) {
    if (src != null) AsyncImage(
        model = src,
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
        contentScale = ContentScale.FillWidth
    ) else Box(
        modifier = modifier
            .aspectRatio(1.5F)
            .fillMaxWidth()
            .background(
                color = LocalAppColors.current.backgroundPrimary,
            )
            .border(
                3.dp,
                LocalAppColors.current.backgroundSecondary,
                shape
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