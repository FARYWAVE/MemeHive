package com.farywave.memehive.ui.component

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.farywave.memehive.R
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.ui.simple_component.NullableImage
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun ImagePicker(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    initialSrc: Uri?,
    enabled: Boolean = true,
    onImageSelected: (Uri?) -> Unit
) {
    val launcher = DeviceTools.requestMedia { uri ->
        if (enabled && uri != null) onImageSelected(uri)
    }
    val interactionSource = remember { MutableInteractionSource() }

    ConstraintLayout(
        modifier = modifier.wrapContentSize().clip(shape)
    ) {
        val (image, clear) = createRefs()
        NullableImage(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = {
                    launcher.launch("image/*")
                })
                .constrainAs(image) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            shape,
            initialSrc
        )

        if (initialSrc != null) Box(
            modifier = Modifier
                .size(24.dp)
                .constrainAs(clear) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                }
                .clip(RoundedCornerShape(0.dp, 0.dp, 0.dp, 10.dp))
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = ripple(
                        bounded = true,
                        color = LocalAppColors.current.contentPrimary
                    ),
                    onClick = {
                        onImageSelected(null)
                    }
                )
                .background(LocalAppColors.current.warning),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(20.dp).padding(2.dp),
                painter = painterResource(id = R.drawable.ic_cross),
                contentDescription = stringResource(R.string.remove_image),
                tint = LocalAppColors.current.contentPrimary
            )
        }
    }

}
