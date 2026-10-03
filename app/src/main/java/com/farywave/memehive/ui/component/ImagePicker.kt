package com.farywave.memehive.ui.component

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.ui.simple_component.NullableImage

@Composable
fun ImagePicker(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    initialSrc: Uri?,
    onImageSelected: (Uri?) -> Unit
) {
    val launcher = DeviceTools.requestMedia { onImageSelected(it) }
    NullableImage(
        modifier
            .fillMaxWidth()
            .clickable(onClick = {
                launcher.launch("image/*")
            }),
        shape,
        initialSrc
    )
}