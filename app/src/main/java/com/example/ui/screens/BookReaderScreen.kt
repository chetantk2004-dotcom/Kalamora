package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Book
import com.example.data.model.Chapter
import com.example.data.model.UiState
import com.example.ui.components.BookCoverArt
import com.example.ui.components.KalamoraErrorBanner
import com.example.ui.components.LiteraryRichTextView
import com.example.ui.components.ShareUtil
import com.example.ui.components.VisibilityBadge
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.literaryFontFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(
    bookState: UiState<Book?>,
    chaptersState: UiState<List<Chapter>>,
    onNavigateBack: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current

    var currentChapterIndex by remember { mutableIntStateOf(0) }
    var fontSizeSp by remember { mutableFloatStateOf(17f) }
    var showTocSheet by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    BackHandler { onNavigateBack() }

    LaunchedEffect(currentChapterIndex) {
        scrollState.scrollTo(0)
    }

    val book = (bookState as? UiState.Success)?.data
    val chapters = (chaptersState as? UiState.Success)?.data.orEmpty()
    val safeIndex = currentChapterIndex.coerceIn(0, (chapters.size - 1).coerceAtLeast(0))
    val currentChapter = chapters.getOrNull(safeIndex)

    val readingProgress = if (chapters.isEmpty()) 0f else (safeIndex + 1f) / chapters.size.toFloat()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text = book?.title ?: strings.publicLibraryTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    TextButton(onClick = { fontSizeSp = (fontSizeSp - 2f).coerceAtLeast(13f) }) {
                        Text("A-")
                    }
                    TextButton(onClick = { fontSizeSp = (fontSizeSp + 2f).coerceAtMost(26f) }) {
                        Text("A+")
                    }
                    if (chapters.isNotEmpty()) {
                        IconButton(
                            onClick = { showTocSheet = true },
                            modifier = Modifier.testTag("reader_chapters_button")
                        ) {
                            Icon(Icons.Filled.FormatListBulleted, contentDescription = strings.chaptersDrawerTitle)
                        }
                    }
                    if (book != null && book.isPublicBook()) {
                        IconButton(
                            onClick = {
                                ShareUtil.shareText(
                                    context = context,
                                    title = book.title,
                                    body = "Read \"${book.title}\" by ${book.authorName} on Kalamora!\n\n${book.description}\n\nLink: https://kalamora.app/book/${book.id}"
                                )
                            }
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = strings.shareLabel)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (chapters.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { readingProgress },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when {
                bookState is UiState.Loading || chaptersState is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                bookState is UiState.Error -> {
                    Box(Modifier.padding(20.dp)) {
                        KalamoraErrorBanner(message = bookState.message)
                    }
                }
                chaptersState is UiState.Error -> {
                    Box(Modifier.padding(20.dp)) {
                        KalamoraErrorBanner(message = chaptersState.message)
                    }
                }
                book == null -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("This book is private or no longer available.")
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 720.dp)
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(horizontal = 24.dp, vertical = 20.dp)
                        ) {
                            // Book Cover Header on Chapter 1
                            if (safeIndex == 0) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BookCoverArt(
                                            title = book.title,
                                            authorName = book.authorName,
                                            coverTheme = book.coverTheme,
                                            coverURL = book.coverURL,
                                            modifier = Modifier.width(86.dp).height(124.dp)
                                        )
                                        Spacer(Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            VisibilityBadge(isPublic = book.isPublicBook())
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = book.title,
                                                style = MaterialTheme.typography.headlineSmall.copy(
                                                    fontFamily = literaryFontFor(book.title, book.language)
                                                )
                                            )
                                            if (!book.subtitle.isNullOrBlank()) {
                                                Text(
                                                    text = book.subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "${strings.byAuthorPrefix} ${book.authorName}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "${book.category} • ${book.language}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                if (book.description.isNotBlank()) {
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = book.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(22.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(22.dp))
                            }

                            if (currentChapter != null) {
                                Text(
                                    text = "${strings.readingProgress}: ${safeIndex + 1} / ${chapters.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = currentChapter.title,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = literaryFontFor(currentChapter.title, book.language)
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(Modifier.height(18.dp))

                                LiteraryRichTextView(
                                    content = currentChapter.content.ifBlank { "(This chapter is currently empty.)" },
                                    languageHint = book.language,
                                    fontSize = fontSizeSp.sp
                                )

                                Spacer(Modifier.height(32.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(16.dp))

                                // Previous / Next Chapter Navigation
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(
                                        onClick = { currentChapterIndex = (safeIndex - 1).coerceAtLeast(0) },
                                        enabled = safeIndex > 0,
                                        modifier = Modifier.testTag("reader_prev_chapter")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.previousChapter)
                                    }
                                    Button(
                                        onClick = { currentChapterIndex = (safeIndex + 1).coerceAtMost(chapters.lastIndex) },
                                        enabled = safeIndex < chapters.lastIndex,
                                        modifier = Modifier.testTag("reader_next_chapter")
                                    ) {
                                        Text(strings.nextChapter)
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                                    }
                                }
                            } else {
                                Text(
                                    text = "No chapters available in this book yet.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }

    if (showTocSheet) {
        ModalBottomSheet(onDismissRequest = { showTocSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(strings.chaptersDrawerTitle, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(chapters, key = { _, ch -> ch.id }) { idx, ch ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (idx == safeIndex) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentChapterIndex = idx
                                    showTocSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}. ${ch.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${ch.wordCount} ${strings.wordsLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
