package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Book
import com.example.data.model.Chapter
import com.example.data.model.SaveStatus
import com.example.data.model.TextStatsUtil
import com.example.data.model.UiState
import com.example.ui.components.DateFormatUtil
import com.example.ui.components.KalamoraErrorBanner
import com.example.ui.components.LiteraryRichTextView
import com.example.ui.components.RichFormatAction
import com.example.ui.components.RichTextEditorOps
import com.example.ui.components.SaveStatusPill
import com.example.ui.components.VisibilityBadge
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.literaryFontFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookWritingStudioScreen(
    book: Book,
    chaptersState: UiState<List<Chapter>>,
    saveStatus: SaveStatus,
    onMarkUnsaved: () -> Unit,
    onResetSaveStatus: () -> Unit,
    onSaveChapter: suspend (chapterId: String, title: String, content: String) -> Boolean,
    onAddChapter: (title: String, onAdded: (String) -> Unit) -> Unit,
    onReorderChapter: (chapterId: String, moveUp: Boolean) -> Unit,
    onDeleteChapter: (chapterId: String, onDeleted: () -> Unit) -> Unit,
    onEditBookDetails: () -> Unit,
    onToggleVisibility: (Boolean) -> Unit,
    onOpenFullBookPreview: () -> Unit,
    onDeleteBook: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    val chapters = (chaptersState as? UiState.Success)?.data.orEmpty()
    var selectedChapterId by remember(book.id) { mutableStateOf<String?>(null) }

    // Editor states for the currently selected chapter
    var chapterTitle by remember(book.id) { mutableStateOf("") }
    var contentField by remember(book.id) { mutableStateOf(TextFieldValue("")) }
    var lastSyncedTitle by remember(book.id) { mutableStateOf("") }
    var lastSyncedContent by remember(book.id) { mutableStateOf("") }
    var hydratedChapterId by remember(book.id) { mutableStateOf<String?>(null) }

    // Undo / Redo stacks
    val undoStack = remember(book.id) { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember(book.id) { mutableStateListOf<TextFieldValue>() }
    var textAlign by remember { mutableStateOf(TextAlign.Start) }

    var isPreviewMode by remember { mutableStateOf(false) }
    var showChaptersSheet by remember { mutableStateOf(false) }
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var renameTargetChapter by remember { mutableStateOf<Chapter?>(null) }
    var deleteTargetChapter by remember { mutableStateOf<Chapter?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showPublishConfirm by remember { mutableStateOf(false) }
    var showDeleteBookConfirm by remember { mutableStateOf(false) }
    var showUnsavedExitDialog by remember { mutableStateOf(false) }

    val isDirty = hydratedChapterId != null &&
        (chapterTitle != lastSyncedTitle || contentField.text != lastSyncedContent)

    // Auto-select first chapter once chapters arrive
    LaunchedEffect(chapters) {
        if (chapters.isNotEmpty() && (selectedChapterId == null || chapters.none { it.id == selectedChapterId })) {
            selectedChapterId = chapters.first().id
        }
    }

    // Hydrate editor when selectedChapterId changes (never overwrite live unsaved typing of the same chapter!)
    val selectedChapter = chapters.firstOrNull { it.id == selectedChapterId }
    LaunchedEffect(selectedChapter?.id) {
        val ch = selectedChapter ?: return@LaunchedEffect
        if (hydratedChapterId != ch.id) {
            hydratedChapterId = ch.id
            chapterTitle = ch.title
            contentField = TextFieldValue(ch.content)
            lastSyncedTitle = ch.title
            lastSyncedContent = ch.content
            undoStack.clear()
            redoStack.clear()
            onResetSaveStatus()
        }
    }

    // Debounced auto-save after 1.6s of inactivity
    LaunchedEffect(hydratedChapterId, chapterTitle, contentField.text) {
        val activeId = hydratedChapterId ?: return@LaunchedEffect
        if (chapterTitle == lastSyncedTitle && contentField.text == lastSyncedContent) return@LaunchedEffect
        onMarkUnsaved()
        delay(1600L)
        val snapTitle = chapterTitle
        val snapContent = contentField.text
        val ok = onSaveChapter(activeId, snapTitle, snapContent)
        if (ok) {
            lastSyncedTitle = snapTitle
            lastSyncedContent = snapContent
        }
    }

    fun triggerManualSave(onDone: (Boolean) -> Unit = {}) {
        val activeId = hydratedChapterId ?: return
        scope.launch {
            val snapTitle = chapterTitle
            val snapContent = contentField.text
            val ok = onSaveChapter(activeId, snapTitle, snapContent)
            if (ok) {
                lastSyncedTitle = snapTitle
                lastSyncedContent = snapContent
            }
            onDone(ok)
        }
    }

    // Flush current chapter cleanly before switching to another chapter — NEVER lose content!
    fun switchToChapter(targetId: String) {
        if (targetId == selectedChapterId) {
            showChaptersSheet = false
            return
        }
        val activeId = hydratedChapterId
        if (activeId != null && isDirty) {
            scope.launch {
                val snapTitle = chapterTitle
                val snapContent = contentField.text
                val ok = onSaveChapter(activeId, snapTitle, snapContent)
                if (ok) {
                    lastSyncedTitle = snapTitle
                    lastSyncedContent = snapContent
                    selectedChapterId = targetId
                    showChaptersSheet = false
                }
            }
        } else {
            selectedChapterId = targetId
            showChaptersSheet = false
        }
    }

    fun requestBack() {
        if (isDirty) {
            showUnsavedExitDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler { requestBack() }

    val liveWordCount = remember(contentField.text) { TextStatsUtil.countWords(contentField.text) }
    val liveCharCount = remember(contentField.text) { TextStatsUtil.countCharacters(contentField.text) }

    fun pushUndoAndUpdate(newValue: TextFieldValue) {
        if (newValue.text != contentField.text) {
            if (undoStack.size > 40) undoStack.removeAt(0)
            undoStack.add(contentField)
            redoStack.clear()
        }
        contentField = newValue
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { requestBack() },
                        modifier = Modifier.testTag("studio_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SaveStatusPill(
                                status = saveStatus,
                                onRetry = { triggerManualSave() }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { triggerManualSave() },
                        modifier = Modifier.testTag("studio_manual_save_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = strings.saveLabel)
                    }
                    IconButton(
                        onClick = { isPreviewMode = !isPreviewMode },
                        modifier = Modifier.testTag("studio_preview_toggle")
                    ) {
                        Icon(
                            imageVector = if (isPreviewMode) Icons.Filled.Edit else Icons.Filled.Visibility,
                            contentDescription = if (isPreviewMode) strings.editLabel else strings.chapterPreview
                        )
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Studio options")
                        }
                        DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(strings.fullBookPreview) },
                                leadingIcon = { Icon(Icons.Filled.Visibility, null) },
                                onClick = {
                                    showOverflowMenu = false
                                    if (isDirty) triggerManualSave { onOpenFullBookPreview() }
                                    else onOpenFullBookPreview()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.editBookDetails) },
                                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                onClick = { showOverflowMenu = false; onEditBookDetails() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (book.isPublicBook()) strings.makePrivate else strings.publishBook) },
                                leadingIcon = {
                                    Icon(if (book.isPublicBook()) Icons.Filled.Lock else Icons.Filled.Public, null)
                                },
                                onClick = { showOverflowMenu = false; showPublishConfirm = true }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.deleteLabel, color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { showOverflowMenu = false; showDeleteBookConfirm = true }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideStudio = maxWidth >= 840.dp

            when (chaptersState) {
                UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.padding(20.dp)) {
                        KalamoraErrorBanner(message = chaptersState.message)
                    }
                }
                is UiState.Success -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // LEFT SIDEBAR (Wide screens: persistent Chapter List)
                        if (isWideStudio) {
                            ChapterListPanel(
                                book = book,
                                chapters = chapters,
                                selectedChapterId = selectedChapterId,
                                onSelectChapter = ::switchToChapter,
                                onAddChapter = { showAddChapterDialog = true },
                                onRenameChapter = { renameTargetChapter = it },
                                onReorderChapter = { id, up -> onReorderChapter(id, up) },
                                onDeleteChapter = { deleteTargetChapter = it },
                                modifier = Modifier
                                    .width(250.dp)
                                    .fillMaxHeight()
                            )
                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        // MAIN WRITING AREA
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            // Compact chapter bar + live count bar
                            if (!isWideStudio) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        TextButton(
                                            onClick = { showChaptersSheet = true },
                                            modifier = Modifier.testTag("open_chapters_sheet_button")
                                        ) {
                                            Icon(Icons.Filled.Menu, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${strings.chaptersDrawerTitle} (${chapters.size})",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                        Text(
                                            text = "$liveWordCount ${strings.wordsLabel} • $liveCharCount ${strings.charactersLabel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Rich Text Formatting Toolbar (when editing)
                            if (!isPreviewMode && selectedChapter != null) {
                                StudioFormattingToolbar(
                                    canUndo = undoStack.isNotEmpty(),
                                    canRedo = redoStack.isNotEmpty(),
                                    currentAlign = textAlign,
                                    onAction = { action ->
                                        pushUndoAndUpdate(RichTextEditorOps.applyFormat(contentField, action))
                                    },
                                    onAlignChange = { textAlign = it },
                                    onUndo = {
                                        if (undoStack.isNotEmpty()) {
                                            redoStack.add(contentField)
                                            contentField = undoStack.removeAt(undoStack.lastIndex)
                                        }
                                    },
                                    onRedo = {
                                        if (redoStack.isNotEmpty()) {
                                            undoStack.add(contentField)
                                            contentField = redoStack.removeAt(redoStack.lastIndex)
                                        }
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }

                            if (selectedChapter == null) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "No chapters yet",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(onClick = { showAddChapterDialog = true }) {
                                            Icon(Icons.Filled.Add, null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(strings.addChapter)
                                        }
                                    }
                                }
                            } else if (isPreviewMode) {
                                // Chapter Preview Mode
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 24.dp, vertical = 20.dp)
                                ) {
                                    Text(
                                        text = chapterTitle,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontFamily = literaryFontFor(chapterTitle, book.language)
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(modifier = Modifier.height(18.dp))
                                    LiteraryRichTextView(
                                        content = contentField.text,
                                        languageHint = book.language,
                                        textAlign = textAlign
                                    )
                                    Spacer(modifier = Modifier.height(48.dp))
                                }
                            } else {
                                // Distraction-Free Writing Canvas
                                val editorFont = literaryFontFor(contentField.text.ifBlank { chapterTitle }, book.language)
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 22.dp, vertical = 18.dp)
                                ) {
                                    BasicTextField(
                                        value = chapterTitle,
                                        onValueChange = { if (it.length <= 200) chapterTitle = it },
                                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                                            fontFamily = editorFont,
                                            color = MaterialTheme.colorScheme.onBackground
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        decorationBox = { inner ->
                                            if (chapterTitle.isEmpty()) {
                                                Text(
                                                    text = strings.chapterTitleHint,
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                            inner()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("chapter_title_input")
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    BasicTextField(
                                        value = contentField,
                                        onValueChange = { newVal ->
                                            if (newVal.text.length <= 200000) {
                                                pushUndoAndUpdate(newVal)
                                            }
                                        },
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = editorFont,
                                            fontSize = 17.sp,
                                            lineHeight = 29.sp,
                                            textAlign = textAlign,
                                            color = MaterialTheme.colorScheme.onBackground
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        decorationBox = { inner ->
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                if (contentField.text.isEmpty()) {
                                                    Text(
                                                        text = strings.chapterContentHint,
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontFamily = editorFont,
                                                            fontSize = 17.sp
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                                    )
                                                }
                                                inner()
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(480.dp)
                                            .testTag("chapter_content_input")
                                    )
                                    Spacer(modifier = Modifier.height(60.dp))
                                }
                            }
                        }

                        // RIGHT INSPECTOR PANEL (Desktop / Expanded Tablet)
                        if (isWideStudio) {
                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(
                                modifier = Modifier
                                    .width(220.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Document Info",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                VisibilityBadge(isPublic = book.isPublicBook())
                                InspectorMetric(label = strings.wordsLabel, value = liveWordCount.toString())
                                InspectorMetric(label = strings.charactersLabel, value = liveCharCount.toString())
                                InspectorMetric(label = strings.writingLanguageLabel, value = book.language)
                                InspectorMetric(
                                    label = "Last Edited",
                                    value = DateFormatUtil.formatDateTime(selectedChapter?.updatedAt ?: book.updatedAt)
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                TextButton(
                                    onClick = { showPublishConfirm = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (book.isPublicBook()) Icons.Filled.Lock else Icons.Filled.Public,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (book.isPublicBook()) strings.makePrivate else strings.publishBook)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Mobile Chapters Bottom Sheet
    if (showChaptersSheet) {
        ModalBottomSheet(onDismissRequest = { showChaptersSheet = false }) {
            ChapterListPanel(
                book = book,
                chapters = chapters,
                selectedChapterId = selectedChapterId,
                onSelectChapter = ::switchToChapter,
                onAddChapter = {
                    showChaptersSheet = false
                    showAddChapterDialog = true
                },
                onRenameChapter = {
                    showChaptersSheet = false
                    renameTargetChapter = it
                },
                onReorderChapter = { id, up -> onReorderChapter(id, up) },
                onDeleteChapter = {
                    showChaptersSheet = false
                    deleteTargetChapter = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            )
        }
    }

    // Add Chapter Dialog
    if (showAddChapterDialog) {
        var newTitle by remember { mutableStateOf("Chapter ${chapters.size + 1}") }
        AlertDialog(
            onDismissRequest = { showAddChapterDialog = false },
            title = { Text(strings.addChapter) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { if (it.length <= 200) newTitle = it },
                    label = { Text(strings.chapterTitleHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_chapter_title_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val safeTitle = newTitle.trim().ifBlank { "Chapter ${chapters.size + 1}" }
                        showAddChapterDialog = false
                        val flushThenAdd = {
                            onAddChapter(safeTitle) { createdId ->
                                selectedChapterId = createdId
                            }
                        }
                        if (isDirty) triggerManualSave { flushThenAdd() } else flushThenAdd()
                    },
                    modifier = Modifier.testTag("confirm_add_chapter_button")
                ) {
                    Text(strings.addChapter)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChapterDialog = false }) {
                    Text(strings.cancelLabel)
                }
            }
        )
    }

    // Rename Chapter Dialog
    renameTargetChapter?.let { ch ->
        var editedTitle by remember(ch.id) { mutableStateOf(ch.title) }
        AlertDialog(
            onDismissRequest = { renameTargetChapter = null },
            title = { Text(strings.renameChapter) },
            text = {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { if (it.length <= 200) editedTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val safe = editedTitle.trim().ifBlank { ch.title }
                        renameTargetChapter = null
                        if (ch.id == selectedChapterId) {
                            chapterTitle = safe
                            triggerManualSave()
                        } else {
                            scope.launch {
                                onSaveChapter(ch.id, safe, ch.content)
                            }
                        }
                    }
                ) {
                    Text(strings.saveLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetChapter = null }) {
                    Text(strings.cancelLabel)
                }
            }
        )
    }

    // Delete Chapter Confirmation
    deleteTargetChapter?.let { ch ->
        AlertDialog(
            onDismissRequest = { deleteTargetChapter = null },
            title = { Text(strings.deleteChapter) },
            text = { Text("Delete \"${ch.title}\"? This chapter cannot be recovered.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetId = ch.id
                        deleteTargetChapter = null
                        onDeleteChapter(targetId) {
                            if (selectedChapterId == targetId) {
                                hydratedChapterId = null
                                selectedChapterId = chapters.firstOrNull { it.id != targetId }?.id
                            }
                        }
                    }
                ) {
                    Text(strings.deleteLabel, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetChapter = null }) {
                    Text(strings.cancelLabel)
                }
            }
        )
    }

    // Publish / Unpublish Confirmation
    if (showPublishConfirm) {
        val targetPublic = !book.isPublicBook()
        AlertDialog(
            onDismissRequest = { showPublishConfirm = false },
            title = { Text(if (targetPublic) strings.publishConfirmTitle else strings.unpublishConfirmTitle) },
            text = { Text(if (targetPublic) strings.publishConfirmBody else strings.unpublishConfirmBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPublishConfirm = false
                        onToggleVisibility(targetPublic)
                    }
                ) {
                    Text(if (targetPublic) strings.publishBook else strings.makePrivate)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishConfirm = false }) {
                    Text(strings.cancelLabel)
                }
            }
        )
    }

    // Delete Book Confirmation
    if (showDeleteBookConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteBookConfirm = false },
            title = { Text(strings.deleteBookTitle) },
            text = { Text(strings.deleteBookBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteBookConfirm = false
                        onDeleteBook()
                    }
                ) {
                    Text(strings.deleteLabel, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteBookConfirm = false }) {
                    Text(strings.cancelLabel)
                }
            }
        )
    }

    // Unsaved Changes Exit Guard
    if (showUnsavedExitDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedExitDialog = false },
            title = { Text(strings.unsavedChangesTitle) },
            text = { Text(strings.unsavedChangesBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedExitDialog = false
                        triggerManualSave { saved ->
                            if (saved) onNavigateBack()
                        }
                    }
                ) {
                    Text(strings.saveAndLeave)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showUnsavedExitDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text(strings.discardChanges, color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showUnsavedExitDialog = false }) {
                        Text(strings.stayHere)
                    }
                }
            }
        )
    }
}

@Composable
private fun ChapterListPanel(
    book: Book,
    chapters: List<Chapter>,
    selectedChapterId: String?,
    onSelectChapter: (String) -> Unit,
    onAddChapter: () -> Unit,
    onRenameChapter: (Chapter) -> Unit,
    onReorderChapter: (String, Boolean) -> Unit,
    onDeleteChapter: (Chapter) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Column(modifier = modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.chaptersDrawerTitle,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${book.totalWordCount} ${strings.wordsLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onAddChapter,
                modifier = Modifier.testTag("add_chapter_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.addChapter)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(chapters, key = { _, ch -> ch.id }) { index, chapter ->
                val isSelected = chapter.id == selectedChapterId
                var menuOpen by remember { mutableStateOf(false) }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectChapter(chapter.id) }
                        .testTag("chapter_row_${chapter.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}. ${chapter.title}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${chapter.wordCount} ${strings.wordsLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { menuOpen = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Chapter actions", modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(strings.renameChapter) },
                                    leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                    onClick = { menuOpen = false; onRenameChapter(chapter) }
                                )
                                if (index > 0) {
                                    DropdownMenuItem(
                                        text = { Text(strings.moveUp) },
                                        leadingIcon = { Icon(Icons.Filled.ArrowUpward, null) },
                                        onClick = { menuOpen = false; onReorderChapter(chapter.id, true) }
                                    )
                                }
                                if (index < chapters.lastIndex) {
                                    DropdownMenuItem(
                                        text = { Text(strings.moveDown) },
                                        leadingIcon = { Icon(Icons.Filled.ArrowDownward, null) },
                                        onClick = { menuOpen = false; onReorderChapter(chapter.id, false) }
                                    )
                                }
                                if (chapters.size > 1) {
                                    DropdownMenuItem(
                                        text = { Text(strings.deleteChapter, color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = { menuOpen = false; onDeleteChapter(chapter) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioFormattingToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    currentAlign: TextAlign,
    onAction: (RichFormatAction) -> Unit,
    onAlignChange: (TextAlign) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ToolbarIconBtn(Icons.AutoMirrored.Filled.Undo, "Undo", enabled = canUndo, onClick = onUndo)
        ToolbarIconBtn(Icons.AutoMirrored.Filled.Redo, "Redo", enabled = canRedo, onClick = onRedo)
        ToolbarIconBtn(Icons.Filled.FormatBold, "Bold") { onAction(RichFormatAction.BOLD) }
        ToolbarIconBtn(Icons.Filled.FormatItalic, "Italic") { onAction(RichFormatAction.ITALIC) }
        ToolbarIconBtn(Icons.Filled.FormatUnderlined, "Underline") { onAction(RichFormatAction.UNDERLINE) }
        ToolbarIconBtn(Icons.Filled.Title, "Heading") { onAction(RichFormatAction.HEADING) }
        TextButton(
            onClick = { onAction(RichFormatAction.PARAGRAPH) },
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text("¶", style = MaterialTheme.typography.titleMedium)
        }
        ToolbarIconBtn(Icons.AutoMirrored.Filled.FormatListBulleted, "Bullet list") { onAction(RichFormatAction.BULLET_LIST) }
        ToolbarIconBtn(Icons.Filled.FormatListNumbered, "Numbered list") { onAction(RichFormatAction.NUMBERED_LIST) }
        ToolbarIconBtn(Icons.Filled.FormatQuote, "Block quote") { onAction(RichFormatAction.QUOTE) }
        ToolbarIconBtn(Icons.Filled.HorizontalRule, "Horizontal divider") { onAction(RichFormatAction.DIVIDER) }
        ToolbarIconBtn(
            icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
            label = "Align Left",
            active = currentAlign == TextAlign.Start
        ) { onAlignChange(TextAlign.Start) }
        ToolbarIconBtn(
            icon = Icons.Filled.FormatAlignCenter,
            label = "Align Center",
            active = currentAlign == TextAlign.Center
        ) { onAlignChange(TextAlign.Center) }
        ToolbarIconBtn(
            icon = Icons.AutoMirrored.Filled.FormatAlignRight,
            label = "Align Right",
            active = currentAlign == TextAlign.End
        ) { onAlignChange(TextAlign.End) }
    }
}

@Composable
private fun ToolbarIconBtn(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    active: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                active -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun InspectorMetric(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
