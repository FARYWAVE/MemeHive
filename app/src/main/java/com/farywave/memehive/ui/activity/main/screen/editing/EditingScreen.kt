package com.farywave.memehive.ui.activity.main.screen.editing

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.farywave.memehive.R
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.ui.activity.main.dialog.UnsavedChangesDialog
import com.farywave.memehive.ui.activity.main.dialog.collection_editor.CollectionDraft
import com.farywave.memehive.ui.activity.main.dialog.collection_editor.CollectionEditorDialog
import com.farywave.memehive.ui.component.AppActionMenu
import com.farywave.memehive.ui.component.AppIconButton
import com.farywave.memehive.ui.component.AppTextField
import com.farywave.memehive.ui.component.CollectionPickerSheet
import com.farywave.memehive.ui.component.CollectionSelectorSheet
import com.farywave.memehive.ui.component.ErrorNotice
import com.farywave.memehive.ui.component.ImagePicker
import com.farywave.memehive.ui.model.MediaItemDraft
import com.farywave.memehive.ui.theme.EditingFieldVerticalPadding
import com.farywave.memehive.ui.theme.LocalAppColors
import com.farywave.memehive.ui.theme.editingFieldShape
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
    onImageChange: (Uri?) -> Unit,
    onCaptionChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onRemoveTag: (Int) -> Unit,
    onUpdateTag: (Int, String) -> Unit,
    onAddTag: (String) -> Unit,
    onCopyImage: (Image) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var collectionSheet by
        rememberSaveable(stateSaver = EditorCollectionSheetSaver) {
            mutableStateOf(EditorCollectionSheet.NONE)
        }

    var showExitConfirmation by rememberSaveable { mutableStateOf(false) }
    val requestClose: () -> Unit = {
        if (!state.isSaving) {
            val hasChanges =
                !state.isLoading &&
                    state.loadError == null &&
                    (state.contentState == EditorContentState.EDITING ||
                        (state.contentState == EditorContentState.CREATION &&
                            !state.mediaItemDraft.hasSameContentAs(MediaItemDraft())))
            if (hasChanges) showExitConfirmation = true else onBack()
        }
    }
    BackHandler(
        enabled =
            state.collectionEditor == null &&
                collectionSheet == EditorCollectionSheet.NONE &&
                !showExitConfirmation,
        onBack = requestClose,
    )

    Scaffold(
        modifier =
            Modifier.fillMaxSize()
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
                modifier =
                    Modifier.fillMaxWidth()
                        .background(LocalAppColors.current.backgroundPrimary)
                        .padding(horizontal = 17.dp, vertical = 6.dp)
                        .statusBarsPadding()
                        .height(46.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIconButton(
                    modifier = Modifier.padding(7.dp).size(30.dp),
                    icon = painterResource(R.drawable.ic_back),
                ) {
                    requestClose()
                }

                Spacer(Modifier.weight(1f))

                if (
                    !state.isLoading &&
                        state.loadError == null &&
                        state.contentState in
                            listOf(EditorContentState.EDITING, EditorContentState.CREATION)
                ) {
                    AppIconButton(
                        modifier = Modifier.padding(7.dp).size(30.dp),
                        icon = painterResource(R.drawable.ic_check),
                    ) {
                        if (!state.isSaving) {
                            focusManager.clearFocus()
                            onSave()
                        }
                    }

                    AppIconButton(
                        modifier = Modifier.padding(7.dp).size(30.dp),
                        icon = painterResource(R.drawable.ic_cross),
                    ) {
                        if (!state.isSaving) onDiscard()
                    }
                }

                AppActionMenu<EditorAction>(
                    options =
                        enumEntries<EditorAction>().filter { option ->
                            if (state.isSaving || state.isLoading || state.loadError != null)
                                return@filter false
                            when (option) {
                                EditorAction.COPY_TO_CLIPBOARD ->
                                    (state.mediaItemDraft.image?.id ?: 0L) != 0L
                                EditorAction.DUPLICATE ->
                                    state.contentState == EditorContentState.SAVED
                                EditorAction.SET_AS_COVER ->
                                    state.contentState == EditorContentState.SAVED &&
                                        state.mediaItemDraft.image != null
                                EditorAction.DELETE ->
                                    state.contentState in
                                        listOf(
                                            EditorContentState.EDITING,
                                            EditorContentState.SAVED,
                                        )

                                else -> true
                            }
                        },
                    onSelected = { action ->
                        when (action) {
                            EditorAction.COPY_TO_CLIPBOARD ->
                                state.mediaItemDraft.image?.let {
                                    onCopyImage(it)
                                }

                            EditorAction.ADD_TO_COLLECTION ->
                                collectionSheet = EditorCollectionSheet.MEMBERSHIP
                            EditorAction.SET_AS_COVER ->
                                collectionSheet = EditorCollectionSheet.COVER
                            EditorAction.DUPLICATE -> if (!state.isSaving) onDuplicate()
                            EditorAction.DELETE -> if (!state.isSaving) onDelete()
                        }
                    },
                ) { onClick ->
                    AppIconButton(
                        modifier = Modifier.padding(7.dp).size(30.dp),
                        icon = painterResource(R.drawable.ic_more),
                    ) {
                        onClick()
                    }
                }
            }
        },
    ) { contentPadding ->
        Box(
            Modifier.fillMaxSize()
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .navigationBarsPadding()
                .imePadding()
                .background(LocalAppColors.current.backgroundPrimary)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
                return@Box
            }
            state.loadError?.let {
                Box(Modifier.padding(16.dp)) {
                    if (it == R.string.error_media_missing)
                        ErrorNotice(it, onBack, R.string.action_back_to_library)
                    else ErrorNotice(it, onRetryLoading)
                }
                return@Box
            }
            val roundedShape = editingFieldShape()

            LazyColumn(
                modifier =
                    Modifier.padding(horizontal = 10.dp)
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium),
                contentPadding = PaddingValues(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.hasCollectionsLoadError)
                    item {
                        ErrorNotice(R.string.error_collections_load, onRetryCollections)
                    }
                item {
                    ImagePicker(
                        imageUri = state.mediaItemDraft.image?.uri,
                        shape = roundedShape,
                        enabled = !state.isSaving,
                    ) { uri ->
                        onImageChange(uri)
                    }
                }
                item {
                    AppTextField(
                        enabled = !state.isSaving,
                        modifier =
                            Modifier.fillMaxWidth()
                                .wrapContentHeight()
                                .background(
                                    LocalAppColors.current.backgroundSecondary,
                                    shape = roundedShape,
                                )
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = EditingFieldVerticalPadding,
                                ),
                        hint = stringResource(R.string.hint_media_caption),
                        initialValue = state.mediaItemDraft.caption,
                        onValueChange = { onCaptionChange(it) },
                        debounceMillis = 0L,
                    )
                }

                item {
                    AppTextField(
                        enabled = !state.isSaving,
                        modifier =
                            Modifier.fillMaxWidth()
                                .wrapContentHeight()
                                .background(
                                    LocalAppColors.current.backgroundSecondary,
                                    shape = roundedShape,
                                )
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = EditingFieldVerticalPadding,
                                ),
                        hint = stringResource(R.string.hint_media_description),
                        minLines = 3,
                        initialValue = state.mediaItemDraft.description,
                        onValueChange = { onDescriptionChange(it) },
                        debounceMillis = 0L,
                    )
                }
                item {
                    Column {
                        Text(
                            modifier = Modifier.padding(bottom = 5.dp, start = 10.dp),
                            text = stringResource(R.string.hint_media_tags),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalAppColors.current.contentSecondary,
                        )
                        FlowRow(
                            modifier =
                                Modifier.fillMaxWidth().wrapContentHeight().padding(horizontal = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            state.mediaItemDraft.tags.forEach { tag ->
                                key(tag.key) {
                                    TagChip(
                                        tag = tag.text,
                                        enabled = !state.isSaving,
                                        onFocusLost = { newText ->
                                            if (newText.isEmpty()) onRemoveTag(tag.key)
                                            else onUpdateTag(tag.key, newText)
                                        },
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
                }
            }
            CollectionSelectorSheet(
                show = collectionSheet == EditorCollectionSheet.MEMBERSHIP,
                collections = state.collections,
                selectedIds = state.mediaItemDraft.collectionIds,
                onDismissRequest = { collectionSheet = EditorCollectionSheet.NONE },
                onCollectionClick = { onToggleCollection(it) },
                onCreateCollection = { onCreateCollection() },
            )

            CollectionPickerSheet(
                show = collectionSheet == EditorCollectionSheet.COVER,
                collections = state.collections,
                onDismissRequest = { collectionSheet = EditorCollectionSheet.NONE },
                onCollectionSelected = {
                    collectionSheet = EditorCollectionSheet.NONE
                    onSetAsCover(it)
                },
                onCreateCollection = { onCreateCollection() },
            )
        }
    }
    if (showExitConfirmation) {
        UnsavedChangesDialog(
            onSave = {
                showExitConfirmation = false
                focusManager.clearFocus()
                onSaveAndClose()
            },
            onDiscard = {
                showExitConfirmation = false
                onBack()
            },
            onDismissRequest = { showExitConfirmation = false },
        )
    }

    CollectionEditorDialog(
        state = state.collectionEditor,
        onSubmit = onSubmitCollection,
        onDismissRequest = onDismissCollection,
    )
}

@Composable
private fun TagChip(tag: String, enabled: Boolean = true, onFocusLost: (tag: String) -> Unit) {
    val value = rememberTextFieldState(tag)
    val focusManager = LocalFocusManager.current

    Box(
        Modifier.wrapContentSize()
            .background(LocalAppColors.current.accentPrimary, shape = MaterialTheme.shapes.large)
    ) {
        BasicTextField(
            enabled = enabled,
            state = value,
            modifier =
                Modifier.width(IntrinsicSize.Min)
                    .widthIn(min = 20.dp)
                    .padding(horizontal = 7.dp, vertical = 6.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused.not()) {
                            val normalizedTag = normalizeTag(value.text.toString())
                            value.setTextAndPlaceCursorAtEnd(normalizedTag)
                            onFocusLost(normalizedTag)
                        }
                    },
            textStyle =
                MaterialTheme.typography.bodyMedium.copy(
                    color = LocalAppColors.current.contentPrimary
                ),
            lineLimits = TextFieldLineLimits.SingleLine,
            cursorBrush = SolidColor(LocalAppColors.current.accentSecondary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onKeyboardAction =
                KeyboardActionHandler {
                    focusManager.clearFocus()
                },
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
                    onFocusLost(normalizeTag(text.toString()))
                    value.setTextAndPlaceCursorAtEnd("")
                }
            }
    }
    Box(
        Modifier.wrapContentSize()
            .background(LocalAppColors.current.accentPrimary, shape = MaterialTheme.shapes.large)
            .widthIn(min = 30.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (value.text.isEmpty() && !isFocused)
            Text(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                text = "+",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAppColors.current.contentPrimary,
            )
        BasicTextField(
            enabled = enabled,
            state = value,
            modifier =
                Modifier.width(IntrinsicSize.Min)
                    .widthIn(min = 20.dp)
                    .padding(horizontal = 7.dp, vertical = 6.dp)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (!isFocused) {
                            onFocusLost(normalizeTag(value.text.toString()))
                            value.setTextAndPlaceCursorAtEnd("")
                        }
                    },
            textStyle =
                MaterialTheme.typography.bodyMedium.copy(
                    color = LocalAppColors.current.contentPrimary
                ),
            lineLimits = TextFieldLineLimits.SingleLine,
            cursorBrush = SolidColor(LocalAppColors.current.accentSecondary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onKeyboardAction =
                KeyboardActionHandler {
                    onFocusLost(normalizeTag(value.text.toString()))
                    value.setTextAndPlaceCursorAtEnd("")
                },
        )
    }
}

private fun normalizeTag(tag: String): String {
    return tag.trim().removeSuffix(",").trim().lowercase().replace(' ', '_')
}

private enum class EditorCollectionSheet(val savedValue: String) {
    NONE("none"),
    MEMBERSHIP("selector"),
    COVER("cover"),
}

private val EditorCollectionSheetSaver =
    Saver<EditorCollectionSheet, String>(
        save = { it.savedValue },
        restore = { saved ->
            EditorCollectionSheet.entries.firstOrNull { it.savedValue == saved }
                ?: EditorCollectionSheet.NONE
        },
    )
