package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.KalamoraCatalog
import com.example.data.model.SaveStatus
import com.example.data.model.Shayari
import com.example.data.model.TextStatsUtil
import com.example.ui.components.SaveStatusPill
import com.example.ui.components.ShareUtil
import com.example.ui.components.VisibilityBadge
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.literaryFontFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShayariStudioScreen(
    existingShayari: Shayari?,
    defaultAuthorName: String,
    saveStatus: SaveStatus,
    onMarkUnsaved: () -> Unit,
    onResetSaveStatus: () -> Unit,
    onSaveShayari: suspend (
        existingId: String?,
        authorName: String,
        title: String,
        content: String,
        category: String,
        language: String,
        makePublic: Boolean
    ) -> Result<String>,
    onDeleteShayari: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentId by remember(existingShayari?.id) { mutableStateOf(existingShayari?.id) }
    var title by remember(existingShayari?.id) { mutableStateOf(existingShayari?.title ?: "") }
    var content by remember(existingShayari?.id) { mutableStateOf(existingShayari?.content ?: "") }
    var category by remember(existingShayari?.id) { mutableStateOf(existingShayari?.category ?: "Love") }
    var language by remember(existingShayari?.id) { mutableStateOf(existingShayari?.language ?: "Hindi") }
    var isPublic by remember(existingShayari?.id) { mutableStateOf(existingShayari?.isPublicShayari() ?: false) }

    var syncedTitle by remember(existingShayari?.id) { mutableStateOf(existingShayari?.title ?: "") }
    var syncedContent by remember(existingShayari?.id) { mutableStateOf(existingShayari?.content ?: "") }
    var syncedCategory by remember(existingShayari?.id) { mutableStateOf(existingShayari?.category ?: "Love") }
    var syncedLanguage by remember(existingShayari?.id) { mutableStateOf(existingShayari?.language ?: "Hindi") }
    var syncedPublic by remember(existingShayari?.id) { mutableStateOf(existingShayari?.isPublicShayari() ?: false) }

    var isPreviewMode by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showPublishConfirm by remember { mutableStateOf<Boolean?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showUnsavedExit by remember { mutableStateOf(false) }

    LaunchedEffect(existingShayari?.id) {
        onResetSaveStatus()
    }

    val isDirty = title != syncedTitle ||
        content != syncedContent ||
        category != syncedCategory ||
        language != syncedLanguage ||
        isPublic != syncedPublic

    // Auto-save for existing shayari drafts/works when both title & content are non-blank
    LaunchedEffect(currentId, title, content, category, language) {
        val activeId = currentId ?: return@LaunchedEffect
        if (!isDirty) return@LaunchedEffect
        if (title.isBlank() || content.isBlank()) return@LaunchedEffect
        onMarkUnsaved()
        delay(1800L)
        val snapTitle = title
        val snapContent = content
        val snapCat = category
        val snapLang = language
        val snapPub = isPublic
        val res = onSaveShayari(activeId, defaultAuthorName, snapTitle, snapContent, snapCat, snapLang, snapPub)
        if (res.isSuccess) {
            syncedTitle = snapTitle
            syncedContent = snapContent
            syncedCategory = snapCat
            syncedLanguage = snapLang
            syncedPublic = snapPub
        }
    }

    fun persistNow(targetPublic: Boolean = isPublic, onDone: (Boolean) -> Unit = {}) {
        if (title.trim().isEmpty()) {
            validationError = "Please add a title for your shayari."
            onDone(false)
            return
        }
        if (content.isBlank()) {
            validationError = "Please write at least one line before saving."
            onDone(false)
            return
        }
        validationError = null
        scope.launch {
            val res = onSaveShayari(
                currentId,
                defaultAuthorName,
                title,
                content, // Preserve exact line breaks and words untouched
                category,
                language,
                targetPublic
            )
            res.onSuccess { savedId ->
                currentId = savedId
                isPublic = targetPublic
                syncedTitle = title
                syncedContent = content
                syncedCategory = category
                syncedLanguage = language
                syncedPublic = targetPublic
                onDone(true)
            }.onFailure {
                onDone(false)
            }
        }
    }

    fun requestBack() {
        if (isDirty && (title.isNotBlank() || content.isNotBlank())) {
            showUnsavedExit = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler { requestBack() }

    val poeticFont = literaryFontFor(content.ifBlank { title }, language)
    val wordCount = remember(content) { TextStatsUtil.countWords(content) }
    val lineCount = remember(content) { if (content.isEmpty()) 0 else content.split("\n").size }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { requestBack() },
                        modifier = Modifier.testTag("shayari_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = strings.shayariStudioTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        SaveStatusPill(
                            status = saveStatus,
                            onRetry = { persistNow() }
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Icon(
                            imageVector = if (isPreviewMode) Icons.Filled.Edit else Icons.Filled.Visibility,
                            contentDescription = strings.previewLabel
                        )
                    }
                    if (content.isNotBlank()) {
                        IconButton(
                            onClick = {
                                ShareUtil.shareText(
                                    context = context,
                                    title = title.ifBlank { "Kalamora Shayari" },
                                    body = "${title.ifBlank { "" }}\n\n$content\n\n— $defaultAuthorName (via Kalamora)"
                                )
                            }
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = strings.shareLabel)
                        }
                    }
                    if (!currentId.isNullOrBlank()) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = strings.deleteLabel,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status & Live Poetry Counter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    VisibilityBadge(isPublic = isPublic)
                    Text(
                        text = "$lineCount lines • $wordCount ${strings.wordsLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isPreviewMode) {
                    // Serene Poetic Preview Card
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = title.ifBlank { "Untitled" },
                                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = poeticFont),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(
                                modifier = Modifier.width(64.dp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = content,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = poeticFont,
                                    fontSize = 18.sp,
                                    lineHeight = 32.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(22.dp))
                            Text(
                                text = "— $defaultAuthorName",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    // Poetic Paper Writing Canvas (preserves line breaks & Unicode verbatim)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            BasicTextField(
                                value = title,
                                onValueChange = { if (it.length <= 160) title = it; validationError = null },
                                textStyle = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = poeticFont,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (title.isEmpty()) {
                                        Text(
                                            text = strings.shayariTitleHint,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                        )
                                    }
                                    inner()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("shayari_title_input")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(14.dp))
                            BasicTextField(
                                value = content,
                                onValueChange = { if (it.length <= 50000) content = it; validationError = null },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = poeticFont,
                                    fontSize = 18.sp,
                                    lineHeight = 31.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { inner ->
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (content.isEmpty()) {
                                            Text(
                                                text = strings.shayariContentHint,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontFamily = poeticFont,
                                                    fontSize = 17.sp,
                                                    lineHeight = 28.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                            )
                                        }
                                        inner()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .testTag("shayari_content_input")
                            )
                        }
                    }
                }

                // Category Chips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = strings.categoryLabel, style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(KalamoraCatalog.shayariCategories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                // Writing Language Chips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = strings.writingLanguageLabel, style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(KalamoraCatalog.writingLanguages) { lang ->
                            FilterChip(
                                selected = language == lang,
                                onClick = { language = lang },
                                label = { Text(lang) }
                            )
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Explicit Save Private Draft & Publish Publicly Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (isPublic) {
                                showPublishConfirm = false
                            } else {
                                persistNow(targetPublic = false)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("shayari_save_draft_button")
                    ) {
                        Icon(
                            imageVector = if (isPublic) Icons.Filled.Lock else Icons.Filled.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPublic) strings.makePrivate else strings.saveAsDraft)
                    }

                    Button(
                        onClick = {
                            if (!isPublic) {
                                showPublishConfirm = true
                            } else {
                                persistNow(targetPublic = true)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("shayari_publish_button")
                    ) {
                        Icon(
                            imageVector = if (isPublic) Icons.Filled.Save else Icons.Filled.Public,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPublic) strings.saveLabel else strings.publishPublicly)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    showPublishConfirm?.let { targetPublic ->
        AlertDialog(
            onDismissRequest = { showPublishConfirm = null },
            title = { Text(if (targetPublic) strings.publishConfirmTitle else strings.unpublishConfirmTitle) },
            text = { Text(if (targetPublic) strings.publishConfirmBody else strings.unpublishConfirmBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPublishConfirm = null
                        persistNow(targetPublic = targetPublic)
                    }
                ) {
                    Text(if (targetPublic) strings.publishPublicly else strings.makePrivate)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishConfirm = null }) {
                    Text(strings.cancelLabel)
                }
            }
        )
    }

    if (showDeleteConfirm && !currentId.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(strings.deleteShayariTitle) },
            text = { Text(strings.deleteShayariBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = currentId ?: return@TextButton
                        showDeleteConfirm = false
                        onDeleteShayari(id)
                    }
                ) {
                    Text(strings.deleteLabel, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(strings.cancelLabel)
                }
            }
        )
    }

    if (showUnsavedExit) {
        AlertDialog(
            onDismissRequest = { showUnsavedExit = false },
            title = { Text(strings.unsavedChangesTitle) },
            text = { Text(strings.unsavedChangesBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedExit = false
                        persistNow { saved ->
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
                            showUnsavedExit = false
                            onNavigateBack()
                        }
                    ) {
                        Text(strings.discardChanges, color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showUnsavedExit = false }) {
                        Text(strings.stayHere)
                    }
                }
            }
        )
    }
}
