package com.farywave.memehive.ui.main.screens.editing

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.farywave.memehive.R
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.ui.components.CollectionSelectorSheet
import com.farywave.memehive.ui.navigation.NavEvent
import com.farywave.memehive.ui.simple_components.SimpleActionMenu
import com.farywave.memehive.ui.simple_components.SimpleIconButton
import com.farywave.memehive.ui.simple_components.SimpleTextField
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun Editing(mediaItemId: Long, onNavigate: (NavEvent) -> Unit) {
    val context = LocalContext.current
    val viewModel: EditingViewModel = viewModel(
        factory = EditingViewModelFactory(context, mediaItemId)
    )
    val focusManager = LocalFocusManager.current

    BackHandler {
        viewModel.onSave(context)
        onNavigate(NavEvent.Back)
    }
    var showSheet by remember { mutableStateOf(false) }

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
        topBar = {
            Toolbar(
                onExit = { viewModel.onSave(context) },
                onNavigate = onNavigate,
                onAction = { action ->
                    when (action) {
                        MoreActions.ADD_TO_COLLECTION -> {
                            showSheet = true
                        }
                        MoreActions.DUPLICATE -> {
                            viewModel.onDuplicate(context)
                        }

                        MoreActions.DELETE -> {
                            viewModel.onDelete()
                            onNavigate(NavEvent.ToHive)
                        }
                    }
                })
        }
    ) { contentPadding ->
        val collections by viewModel.collections.collectAsState()
        val selectedIds by viewModel.selectedIds.collectAsState()

        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(LocalAppColors.current.backgroundPrimary),
        ) {
            Content(
                viewModel = viewModel,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxSize(),
                onNavigate = onNavigate
            )
            CollectionSelectorSheet(
                show = showSheet,
                collections = collections,
                selectedIds = selectedIds,
                onDismiss = { showSheet = false },
                onCollectionClicked = { viewModel.toggleCollection(it) },
                onNewCollectionClicked = { onNavigate(NavEvent.NewCollectionDialog) }
            )
        }
    }
}


@Composable
private fun Toolbar(
    onExit: () -> Unit,
    onNavigate: (NavEvent) -> Unit,
    onAction: (MoreActions) -> Unit
) {
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
        ) {
            onExit()
            onNavigate(NavEvent.ToHive)
        }

        Spacer(Modifier.weight(1f))

        SimpleActionMenu<MoreActions>(onSelected = { onAction(it) }) { onClick ->
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
private fun Content(
    viewModel: EditingViewModel,
    modifier: Modifier = Modifier,
    onNavigate: (NavEvent) -> Unit
) {
    val mediaItem by viewModel.mediaItem.collectAsState()
    val editableTags by viewModel.editableTags.collectAsState()
    val mediaSrc by viewModel.mediaSrc.collectAsState()

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
    ) {
        item {
            ImagePicker(initialSrc = mediaSrc, shape = roundedShape) { uri ->
                viewModel.updateMediaSrc(uri)
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
                hint = stringResource(R.string.editing_caption_hint),
                initialValue = mediaItem.caption,
                onValueChange = { viewModel.updateName(it) },
                callbackDelay = 0L
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
                initialValue = mediaItem.description,
                onValueChange = { viewModel.updateDescription(it) },
                callbackDelay = 0L
            )
        }
        item {
            Text(
                modifier = Modifier.padding(bottom = 5.dp, start = 10.dp),
                text = stringResource(R.string.editing_tags_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAppColors.current.contentSecondary
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                editableTags.forEach { tag ->
                    key(tag.id) {
                        TagChip(
                            tag = tag.text,
                            onFocusLost = { newText ->
                                if (newText.isEmpty()) viewModel.removeTag(tag.id)
                                else viewModel.updateTag(tag.id, newText)
                            }
                        )
                    }
                }

                NewTagChip { newText ->
                    if (newText.isNotEmpty()) {
                        viewModel.addTag(newText)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(500.dp)) }
    }
}

@Composable
private fun ImagePicker(
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

@Composable
private fun TagChip(tag: String, onFocusLost: (tag: String) -> Unit) {
    val value = rememberTextFieldState(tag)
    val focusManager = LocalFocusManager.current


    Box(
        Modifier
            .wrapContentSize()
            .background(LocalAppColors.current.accentPrimary, shape = MaterialTheme.shapes.large)
    ) {
        BasicTextField(
            state = value,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .widthIn(min = 20.dp)
                .padding(horizontal = 7.dp, vertical = 6.dp)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused.not()) {
                        onFocusLost(value.text.toString().trim().lowercase().replace(' ', '_'))
                    }
                },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = LocalAppColors.current.contentPrimary),
            lineLimits = TextFieldLineLimits.SingleLine,
            cursorBrush = SolidColor(LocalAppColors.current.accentSecondary),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            onKeyboardAction = KeyboardActionHandler {
                focusManager.clearFocus()
            }
        )
    }
}


@Composable
private fun NewTagChip(onFocusLost: (tag: String) -> Unit) {
    val value = rememberTextFieldState()
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { value.text }
            .collect { text ->
                if (text.isNotEmpty() && (text.trim().endsWith(","))) {
                    onFocusLost(
                        text.trim().dropLast(1).toString().trim().lowercase().replace(' ', '_')
                    )
                    value.setTextAndPlaceCursorAtEnd("")
                }
            }
    }
    Box(
        Modifier
            .wrapContentSize()
            .background(LocalAppColors.current.accentPrimary, shape = MaterialTheme.shapes.large)
            .widthIn(min = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        if (value.text.isEmpty() && !isFocused) Text(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            text = "+",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalAppColors.current.contentPrimary,
        )
        BasicTextField(
            state = value,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .widthIn(min = 20.dp)
                .padding(horizontal = 7.dp, vertical = 6.dp)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (!isFocused) {
                        onFocusLost(value.text.toString().trim().lowercase())
                        value.setTextAndPlaceCursorAtEnd("")
                    }
                },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = LocalAppColors.current.contentPrimary),
            lineLimits = TextFieldLineLimits.SingleLine,
            cursorBrush = SolidColor(LocalAppColors.current.accentSecondary),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            onKeyboardAction = KeyboardActionHandler {
                onFocusLost(value.text.toString().trim().lowercase())
                value.setTextAndPlaceCursorAtEnd("")
            }
        )
    }
}