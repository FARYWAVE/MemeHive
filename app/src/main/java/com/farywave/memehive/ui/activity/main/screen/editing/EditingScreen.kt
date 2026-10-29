package com.farywave.memehive.ui.activity.main.screen.editing

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.farywave.memehive.R
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.ui.activity.main.dialog.DiscardEditingConfirmation
import com.farywave.memehive.ui.activity.main.dialog.collection_editor.CollectionDraft
import com.farywave.memehive.ui.activity.main.dialog.collection_editor.CollectionEditorDialog
import com.farywave.memehive.ui.component.CollectionPickerSheet
import com.farywave.memehive.ui.component.CollectionSelectorSheet
import com.farywave.memehive.ui.component.ErrorNotice
import com.farywave.memehive.ui.component.ImagePicker
import com.farywave.memehive.ui.model.MediaItemDraft
import com.farywave.memehive.ui.simple_component.SimpleActionMenu
import com.farywave.memehive.ui.simple_component.SimpleIconButton
import com.farywave.memehive.ui.simple_component.SimpleTextField
import com.farywave.memehive.ui.theme.LocalAppColors
import kotlin.enums.enumEntries

@Composable
fun EditingScreen(
    state: EditingUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSaveAndClose: () -> Unit,
    onRetryLoading: () -> Unit,
    onRetryCollections: () -> Unit,
    onDiscard: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onCreateCollection: () -> Unit,
    onSubmitCollection: (CollectionDraft) -> Unit,
    onDismissCollection: () -> Unit,
    onToggleCollection: (Collection) -> Unit,
    onSetAsCover: (Collection) -> Unit,
    onImageChanged: (Uri?) -> Unit,
    onCaptionChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onRemoveTag: (Int) -> Unit,
    onUpdateTag: (Int, String) -> Unit,
    onAddTag: (String) -> Unit,
    onCopyImage: (Image) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var collectionSheet by rememberSaveable { mutableStateOf<String?>(null) }

    var showExitConfirmation by rememberSaveable { mutableStateOf(false) }
    val requestClose: () -> Unit = {
        if (!state.isSaving) {
            val hasChanges = !state.isLoading && state.loadError == null && (state.screenState == ScreenState.EDITING ||
                    (state.screenState == ScreenState.CREATION &&
                            !state.mediaItemDraft.hasSameContentAs(MediaItemDraft())))
            if (hasChanges) showExitConfirmation = true else onBack()
        }
    }
    BackHandler(
        enabled = state.collectionEditor == null && collectionSheet == null && !showExitConfirmation,
        onBack = requestClose,
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.backgroundPrimary)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        focusManager.clearFocus()
                    }
                )
            },
        contentWindowInsets = WindowInsets(0),
        topBar = {
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
                    requestClose()
                }

                Spacer(Modifier.weight(1f))

                if (!state.isLoading && state.loadError == null && state.screenState in listOf(ScreenState.EDITING, ScreenState.CREATION)) {
                    SimpleIconButton(
                        modifier = Modifier
                            .padding(7.dp)
                            .size(30.dp),
                        icon = painterResource(R.drawable.ic_check),
                    ) {
                        if (!state.isSaving) { focusManager.clearFocus(); onSave() }
                    }

                    SimpleIconButton(
                        modifier = Modifier
                            .padding(7.dp)
                            .size(30.dp),
                        icon = painterResource(R.drawable.ic_cross)
                    ) {
                        if (!state.isSaving) onDiscard()
                    }
                }

                SimpleActionMenu<MoreActions>(
                    options = enumEntries<MoreActions>().filter { option ->
                        if (state.isSaving || state.isLoading || state.loadError != null) return@filter false
                        when (option) {
                            MoreActions.COPY_TO_CLIPBOARD -> (state.mediaItemDraft.image?.id ?: 0L) != 0L
                            MoreActions.DUPLICATE -> state.screenState == ScreenState.SAVED
                            MoreActions.SET_AS_COVER -> state.screenState == ScreenState.SAVED && state.mediaItemDraft.image != null
                            MoreActions.DELETE -> state.screenState in listOf(
                                ScreenState.EDITING,
                                ScreenState.SAVED
                            )

                            else -> true
                        }
                    },
                    onSelected = { action ->
                        when (action) {
                            MoreActions.COPY_TO_CLIPBOARD -> state.mediaItemDraft.image?.let {
                                onCopyImage(it)
                            }

                            MoreActions.ADD_TO_COLLECTION -> collectionSheet = "selector"
                            MoreActions.SET_AS_COVER -> collectionSheet = "cover"
                            MoreActions.DUPLICATE -> if (!state.isSaving) onDuplicate()
                            MoreActions.DELETE -> if (!state.isSaving) onDelete()
                        }
                    }
                ) { onClick ->
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
    ) { contentPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .imePadding()
                .background(LocalAppColors.current.backgroundPrimary),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
                return@Box
            }
            state.loadError?.let {
                Box(Modifier.padding(16.dp)) {
                    if (it == R.string.error_media_missing) ErrorNotice(it, onBack, R.string.action_back_to_library)
                    else ErrorNotice(it, onRetryLoading)
                }
                return@Box
            }
            val textMeasurer = rememberTextMeasurer()
            val size = textMeasurer.measure(
                text = AnnotatedString("Ambatukam"),
                style = MaterialTheme.typography.bodyMedium
            ).size.height / 2

            val roundedShape = RoundedCornerShape(
                with(LocalDensity.current) { size.toDp() } + 7.dp
            )

            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.collectionsFailed) item {
                    ErrorNotice(R.string.error_collections_load, onRetryCollections)
                }
                item {
                    ImagePicker(
                        initialSrc = state.mediaItemDraft.image?.uri,
                        shape = roundedShape,
                        enabled = !state.isSaving,
                    ) { uri ->
                        onImageChanged(uri)
                    }
                }
                item {
                    SimpleTextField(
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .background(
                                LocalAppColors.current.backgroundSecondary,
                                shape = roundedShape,
                            )
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        hint = stringResource(R.string.hint_media_caption),
                        initialValue = state.mediaItemDraft.caption,
                        onValueChange = { onCaptionChanged(it) },
                        callbackDelay = 0L
                    )
                }

                item {
                    SimpleTextField(
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .background(
                                LocalAppColors.current.backgroundSecondary,
                                shape = roundedShape,
                            )
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        hint = stringResource(R.string.hint_media_description),
                        numberOfLines = 3,
                        initialValue = state.mediaItemDraft.description,
                        onValueChange = { onDescriptionChanged(it) },
                        callbackDelay = 0L
                    )
                }
                item {
                    Text(
                        modifier = Modifier.padding(bottom = 5.dp, start = 10.dp),
                        text = stringResource(R.string.hint_media_tags),
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
                        state.mediaItemDraft.tags.forEach { tag ->
                            key(tag.key) {
                                TagChip(
                                    tag = tag.text,
                            enabled = !state.isSaving,
                                    onFocusLost = { newText ->
                                        if (newText.isEmpty()) onRemoveTag(tag.key)
                                        else onUpdateTag(tag.key, newText)
                                    }
                                )
                            }
                        }

                        NewTagChip(enabled = !state.isSaving) { newText ->
                            if (newText.isNotEmpty()) {
                                onAddTag(newText)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(500.dp)) }
            }
            CollectionSelectorSheet(
                show = collectionSheet == "selector",
                collections = state.collections,
                selectedIds = state.mediaItemDraft.collectionIds,
                onDismiss = { collectionSheet = null },
                onCollectionClicked = { onToggleCollection(it) },
                onNewCollectionClicked = { onCreateCollection() }
            )

            CollectionPickerSheet(
                show = collectionSheet == "cover",
                collections = state.collections,
                onDismiss = { collectionSheet = null },
                onCollectionSelected = { collectionSheet = null; onSetAsCover(it) },
                onNewCollectionClicked = { onCreateCollection() }
            )
        }
    }
    if (showExitConfirmation) {
        DiscardEditingConfirmation(
            onSave = {
                showExitConfirmation = false
                focusManager.clearFocus()
                onSaveAndClose()
            },
            onDiscard = {
                showExitConfirmation = false
                onBack()
            },
            onDismissRequest = { showExitConfirmation = false }
        )
    }

    CollectionEditorDialog(
        state = state.collectionEditor,
        onSubmit = onSubmitCollection,
        onDismiss = onDismissCollection,
    )

}

@Composable
private fun TagChip(tag: String, enabled: Boolean = true, onFocusLost: (tag: String) -> Unit) {
    val value = rememberTextFieldState(tag)
    val focusManager = LocalFocusManager.current


    Box(
        Modifier
            .wrapContentSize()
            .background(LocalAppColors.current.accentPrimary, shape = MaterialTheme.shapes.large)
    ) {
        BasicTextField(
            enabled = enabled,
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
private fun NewTagChip(enabled: Boolean = true, onFocusLost: (tag: String) -> Unit) {
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
            enabled = enabled,
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