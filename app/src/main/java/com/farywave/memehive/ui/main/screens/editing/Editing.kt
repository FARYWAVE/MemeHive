package com.farywave.memehive.ui.main.screens.editing

import android.content.Context
import android.graphics.drawable.shapes.Shape
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.farywave.memehive.R
import com.farywave.memehive.core.FileManager
import com.farywave.memehive.ui.components.ActionMenuOptions
import com.farywave.memehive.ui.components.SimpleActionMenu
import com.farywave.memehive.ui.components.SimpleIconButton
import com.farywave.memehive.ui.components.SimpleTextField
import com.farywave.memehive.ui.navigation.NavEvent
import com.farywave.memehive.ui.theme.LocalAppColors
import com.farywave.memehive.ui.theme.MemeHiveTheme
import java.io.File

@Composable
fun Editing(mediaItemId: Long, onNavigate: (NavEvent) -> Unit) {
    val viewModel: EditingViewModel = viewModel()
    val focusManager = LocalFocusManager.current
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.backgroundPrimary)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
            },
        topBar = { Toolbar(onNavigate) }
    ) { contentPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(LocalAppColors.current.backgroundPrimary),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Content(
                viewModel = viewModel,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxSize(),
            )
        }
    }
}


@Composable
private fun Toolbar(onNavigate: (NavEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(LocalAppColors.current.backgroundPrimary)
            .padding(horizontal = 17.dp, vertical = 6.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SimpleIconButton(
            modifier = Modifier
                .padding(7.dp)
                .size(30.dp),
            icon = painterResource(R.drawable.ic_back)
        ) { onNavigate(NavEvent.ToHive) }

        Spacer(Modifier.weight(1f))

        SimpleActionMenu<MoreActions>(onSelected = { action ->
            {}
        }) { onClick ->
            SimpleIconButton(
                modifier = Modifier
                    .padding(7.dp)
                    .size(30.dp),
                icon = painterResource(R.drawable.ic_more)
            ) {
                onClick()
            }
        }
    }
}

@Composable
private fun Content(viewModel: EditingViewModel, modifier: Modifier = Modifier) {
    val mediaItem by viewModel.mediaItem.collectAsState()

    val textMeasurer = rememberTextMeasurer()
    val size = textMeasurer.measure(
        text = AnnotatedString("Ambatukam"),
        style = MaterialTheme.typography.bodyMedium
    ).size.height / 2

    val roundedShape = RoundedCornerShape(
        with(LocalDensity.current) { size.toDp() } + 7.dp
    )

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val currentSrc = mediaItem.src
        item {
            ImagePicker(initialSrc = mediaItem.src, shape = roundedShape) { src ->
                if (src == null && currentSrc != null) FileManager.deleteFromInternalStorage(
                    currentSrc
                )
                viewModel.updateSrc(src)
            }
        }
        item {
            SimpleTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(
                        LocalAppColors.current.backgroundSecondary,
                        shape = roundedShape
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                hint = stringResource(R.string.editing_name_hint),
                initialValue = mediaItem.name,
                onValueChange = { viewModel.updateName(it) },
                callbackDelay = 25L
            )
        }

        item {
            SimpleTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(
                        LocalAppColors.current.backgroundSecondary,
                        shape = roundedShape
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                hint = stringResource(R.string.editing_description_hint),
                numberOfLines = 3,
                initialValue = mediaItem.name,
                onValueChange = { viewModel.updateDescription(it) },
                callbackDelay = 25L
            )
        }
    }
}

@Composable
private fun ImagePicker(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    initialSrc: File?,
    onImageSelected: (File?) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onImageSelected(FileManager.copyToInternalStorage(context, uri))
        else onImageSelected(null)
    }
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
private fun NullableImage(modifier: Modifier, shape: RoundedCornerShape, src: File?) {
    if (src != null) AsyncImage(
        model = src,
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth(),
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


private enum class MoreActions(
    override val label: String,
    override val highlighted: Boolean = false
) : ActionMenuOptions {
    DUPLICATE("Duplicate"),
    ADD_TO_COLLECTION("Add to Collection"),
    SET_AS_COLLECTION_COVER("Set as Collection Cover"),
    DELETE("Delete", true),

}