package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Book
import com.example.data.model.KalamoraCatalog
import com.example.ui.components.BookCoverArt
import com.example.ui.components.ImageCoverUtil
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.KalamoraCoverPalettes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditBookDialog(
    existingBook: Book? = null,
    defaultAuthorName: String,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        subtitle: String?,
        authorName: String,
        description: String,
        category: String,
        language: String,
        coverTheme: String,
        coverURL: String?,
        makePublic: Boolean
    ) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember(existingBook?.id) { mutableStateOf(existingBook?.title ?: "") }
    var subtitle by remember(existingBook?.id) { mutableStateOf(existingBook?.subtitle ?: "") }
    var authorName by remember(existingBook?.id) { mutableStateOf(existingBook?.authorName?.ifBlank { defaultAuthorName } ?: defaultAuthorName) }
    var description by remember(existingBook?.id) { mutableStateOf(existingBook?.description ?: "") }
    var category by remember(existingBook?.id) { mutableStateOf(existingBook?.category ?: "Fiction") }
    var language by remember(existingBook?.id) { mutableStateOf(existingBook?.language ?: "English") }
    var coverTheme by remember(existingBook?.id) { mutableStateOf(existingBook?.coverTheme ?: "indigo_gold") }
    var coverURL by remember(existingBook?.id) { mutableStateOf(existingBook?.coverURL) }
    // Privacy always defaults to Private for new books!
    var makePublic by remember(existingBook?.id) { mutableStateOf(existingBook?.isPublicBook() ?: false) }

    var validationError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker for book covers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val compressed = withContext(Dispatchers.IO) {
                    ImageCoverUtil.uriToCompactDataUri(context, uri)
                }
                if (compressed != null) {
                    coverURL = compressed
                    validationError = null
                } else {
                    validationError = "Could not process selected cover image. Please choose another photo."
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (existingBook == null) strings.createNewBook else strings.editBookDetails,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (!isSubmitting) onDismiss() }) {
                            Icon(Icons.Filled.Close, contentDescription = strings.closeLabel)
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
                        .widthIn(max = 620.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Live Cover Preview + Photo Picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        BookCoverArt(
                            title = title.ifBlank { "Your Book Title" },
                            authorName = authorName.ifBlank { defaultAuthorName },
                            coverTheme = coverTheme,
                            coverURL = coverURL,
                            modifier = Modifier
                                .width(92.dp)
                                .height(132.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = strings.coverStyleLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(KalamoraCoverPalettes) { palette ->
                                    val selected = coverTheme == palette.key && coverURL == null
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(palette.background)
                                            .border(
                                                width = if (selected) 2.5.dp else 1.dp,
                                                color = if (selected) palette.accent else MaterialTheme.colorScheme.outline,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                coverTheme = palette.key
                                                coverURL = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = palette.labelEn,
                                                tint = palette.accent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.uploadCoverPhoto, style = MaterialTheme.typography.labelMedium)
                                }
                                if (coverURL != null) {
                                    TextButton(onClick = { coverURL = null }) {
                                        Text(strings.removeCoverPhoto, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 160) title = it; validationError = null },
                        label = { Text(strings.bookTitleLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("book_title_input")
                    )

                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { if (it.length <= 200) subtitle = it },
                        label = { Text(strings.bookSubtitleLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = authorName,
                        onValueChange = { if (it.length <= 100) authorName = it; validationError = null },
                        label = { Text(strings.authorNameLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("book_author_input")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 2000) description = it },
                        label = { Text(strings.descriptionLabel) },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category Selection
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.categoryLabel,
                            style = MaterialTheme.typography.titleSmall
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(KalamoraCatalog.bookCategories) { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }

                    // Writing Language Selection
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.writingLanguageLabel,
                            style = MaterialTheme.typography.titleSmall
                        )
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

                    // Privacy Selection (only on creation; existing books use explicit Publish/Make Private controls)
                    if (existingBook == null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = strings.privacyLabel,
                                style = MaterialTheme.typography.titleSmall
                            )
                            PrivacyChoiceRow(
                                selected = !makePublic,
                                icon = Icons.Filled.Lock,
                                title = strings.privacyPrivateTitle,
                                subtitle = strings.privacyPrivateDesc,
                                onClick = { makePublic = false }
                            )
                            PrivacyChoiceRow(
                                selected = makePublic,
                                icon = Icons.Filled.Public,
                                title = strings.privacyPublicTitle,
                                subtitle = strings.privacyPublicDesc,
                                onClick = { makePublic = true }
                            )
                        }
                    }

                    if (validationError != null) {
                        Text(
                            text = validationError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            val trimmedTitle = title.trim()
                            val trimmedAuthor = authorName.trim()
                            when {
                                trimmedTitle.isEmpty() -> validationError = "Please enter a book title."
                                trimmedAuthor.isEmpty() -> validationError = "Please enter an author name."
                                else -> {
                                    isSubmitting = true
                                    onSubmit(
                                        trimmedTitle,
                                        subtitle.trim().ifBlank { null },
                                        trimmedAuthor,
                                        description.trim(),
                                        category,
                                        language,
                                        coverTheme,
                                        coverURL,
                                        makePublic
                                    )
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_book_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = if (existingBook == null) strings.createAndStartWriting else strings.saveLabel,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun PrivacyChoiceRow(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
