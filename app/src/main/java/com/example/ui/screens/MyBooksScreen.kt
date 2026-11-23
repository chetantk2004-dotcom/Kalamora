package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Book
import com.example.data.model.KalamoraCatalog
import com.example.data.model.LibraryFilter
import com.example.data.model.SortOption
import com.example.data.model.UiState
import com.example.ui.components.BookCoverArt
import com.example.ui.components.DateFormatUtil
import com.example.ui.components.KalamoraEmptyState
import com.example.ui.components.KalamoraErrorBanner
import com.example.ui.components.VisibilityBadge
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.literaryFontFor
import com.google.firebase.Timestamp

@Composable
fun MyBooksScreen(
    booksState: UiState<List<Book>>,
    onCreateNewBook: () -> Unit,
    onOpenBookStudio: (Book) -> Unit,
    onPreviewBook: (Book) -> Unit,
    onEditBookDetails: (Book) -> Unit,
    onToggleBookVisibility: (Book, Boolean) -> Unit,
    onDeleteBook: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(SortOption.RECENTLY_EDITED) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    var pendingDeleteBook by remember { mutableStateOf<Book?>(null) }
    var pendingVisibilityBook by remember { mutableStateOf<Pair<Book, Boolean>?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
        ) {
            // Search & Sort Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchBooksHint) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = strings.closeLabel)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("books_search_input")
                )

                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier.testTag("books_sort_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort books")
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.sortRecentlyEdited) },
                            onClick = { sortOption = SortOption.RECENTLY_EDITED; sortMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.sortTitleAz) },
                            onClick = { sortOption = SortOption.TITLE_AZ; sortMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.sortNewest) },
                            onClick = { sortOption = SortOption.NEWEST; sortMenuExpanded = false }
                        )
                    }
                }
            }

            // Status Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = activeFilter == LibraryFilter.ALL,
                        onClick = { activeFilter = LibraryFilter.ALL },
                        label = { Text(strings.filterAll) }
                    )
                }
                item {
                    FilterChip(
                        selected = activeFilter == LibraryFilter.DRAFTS,
                        onClick = { activeFilter = LibraryFilter.DRAFTS },
                        label = { Text(strings.filterDrafts) }
                    )
                }
                item {
                    FilterChip(
                        selected = activeFilter == LibraryFilter.PUBLISHED,
                        onClick = { activeFilter = LibraryFilter.PUBLISHED },
                        label = { Text(strings.filterPublished) }
                    )
                }
                item {
                    FilterChip(
                        selected = activeFilter == LibraryFilter.RECENT,
                        onClick = { activeFilter = LibraryFilter.RECENT },
                        label = { Text(strings.filterRecent) }
                    )
                }
            }

            // Category Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text(strings.allCategories) }
                    )
                }
                items(KalamoraCatalog.bookCategories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat) }
                    )
                }
            }

            when (booksState) {
                UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.padding(20.dp)) {
                        KalamoraErrorBanner(message = booksState.message)
                    }
                }
                is UiState.Success -> {
                    val query = searchQuery.trim().lowercase()
                    val filtered = remember(booksState.data, query, activeFilter, selectedCategory, sortOption) {
                        val base = booksState.data.filter { book ->
                            val matchesStatus = when (activeFilter) {
                                LibraryFilter.ALL -> true
                                LibraryFilter.DRAFTS -> !book.isPublicBook()
                                LibraryFilter.PUBLISHED -> book.isPublicBook()
                                LibraryFilter.RECENT -> true
                            }
                            val matchesCategory = selectedCategory == null || book.category == selectedCategory
                            val matchesQuery = query.isEmpty() ||
                                book.title.lowercase().contains(query) ||
                                (book.subtitle?.lowercase()?.contains(query) == true) ||
                                book.description.lowercase().contains(query) ||
                                book.category.lowercase().contains(query) ||
                                book.language.lowercase().contains(query)
                            matchesStatus && matchesCategory && matchesQuery
                        }
                        val sorted = when (sortOption) {
                            SortOption.RECENTLY_EDITED -> base.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                            SortOption.TITLE_AZ -> base.sortedBy { it.title.lowercase() }
                            SortOption.NEWEST -> base.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        }
                        if (activeFilter == LibraryFilter.RECENT) sorted.take(6) else sorted
                    }

                    if (filtered.isEmpty()) {
                        KalamoraEmptyState(
                            illustrationRes = R.drawable.img_empty_books_1795446303800,
                            title = strings.emptyBooksTitle,
                            body = strings.emptyBooksBody,
                            actionLabel = if (booksState.data.isEmpty()) strings.createNewBook else null,
                            onAction = if (booksState.data.isEmpty()) onCreateNewBook else null
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filtered, key = { it.id }) { book ->
                                BookLibraryCard(
                                    book = book,
                                    onOpen = { onOpenBookStudio(book) },
                                    onPreview = { onPreviewBook(book) },
                                    onEditDetails = { onEditBookDetails(book) },
                                    onToggleVisibility = { makePublic ->
                                        pendingVisibilityBook = Pair(book, makePublic)
                                    },
                                    onDelete = { pendingDeleteBook = book }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(76.dp)) }
                        }
                    }
                }
            }
        }
    }

    pendingDeleteBook?.let { book ->
        AlertDialog(
            onDismissRequest = { pendingDeleteBook = null },
            title = { Text(strings.deleteBookTitle) },
            text = { Text("${book.title}\n\n${strings.deleteBookBody}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteBook(book)
                        pendingDeleteBook = null
                    }
                ) {
                    Text(strings.deleteLabel, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteBook = null }) {
                    Text(strings.cancelLabel)
                }
            }
        )
    }

    pendingVisibilityBook?.let { (book, makePublic) ->
        AlertDialog(
            onDismissRequest = { pendingVisibilityBook = null },
            title = { Text(if (makePublic) strings.publishConfirmTitle else strings.unpublishConfirmTitle) },
            text = { Text(if (makePublic) strings.publishConfirmBody else strings.unpublishConfirmBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onToggleBookVisibility(book, makePublic)
                        pendingVisibilityBook = null
                    }
                ) {
                    Text(if (makePublic) strings.publishBook else strings.makePrivate)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingVisibilityBook = null }) {
                    Text(strings.cancelLabel)
                }
            }
        )
    }
}

@Composable
private fun BookLibraryCard(
    book: Book,
    onOpen: () -> Unit,
    onPreview: () -> Unit,
    onEditDetails: () -> Unit,
    onToggleVisibility: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalStrings.current
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("book_card_${book.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            BookCoverArt(
                title = book.title,
                authorName = book.authorName,
                coverTheme = book.coverTheme,
                coverURL = book.coverURL,
                modifier = Modifier
                    .width(74.dp)
                    .height(106.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = literaryFontFor(book.title, book.language)
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!book.subtitle.isNullOrBlank()) {
                            Text(
                                text = book.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Box {
                        IconButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Book options")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(strings.editBookDetails) },
                                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                onClick = { menuOpen = false; onEditDetails() }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.fullBookPreview) },
                                leadingIcon = { Icon(Icons.Filled.Visibility, null) },
                                onClick = { menuOpen = false; onPreview() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (book.isPublicBook()) strings.makePrivate else strings.publishBook) },
                                leadingIcon = {
                                    Icon(
                                        if (book.isPublicBook()) Icons.Filled.Lock else Icons.Filled.Public,
                                        null
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    onToggleVisibility(!book.isPublicBook())
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.deleteLabel, color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = { menuOpen = false; onDelete() }
                            )
                        }
                    }
                }

                Text(
                    text = "${strings.byAuthorPrefix} ${book.authorName} • ${book.category} • ${book.language}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VisibilityBadge(isPublic = book.isPublicBook())
                    Text(
                        text = "${book.chapterCount} ${strings.chaptersLabel} • ${book.totalWordCount} ${strings.wordsLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = DateFormatUtil.formatDateTime(book.updatedAt ?: book.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
