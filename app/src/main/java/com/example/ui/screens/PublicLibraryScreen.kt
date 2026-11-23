package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Book
import com.example.data.model.KalamoraCatalog
import com.example.data.model.Shayari
import com.example.data.model.UiState
import com.example.ui.components.BookCoverArt
import com.example.ui.components.DateFormatUtil
import com.example.ui.components.KalamoraEmptyState
import com.example.ui.components.KalamoraErrorBanner
import com.example.ui.components.VisibilityBadge
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.literaryFontFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicLibraryScreen(
    publicBooksState: UiState<List<Book>>,
    publicShayariState: UiState<List<Shayari>>,
    onOpenBookReader: (Book) -> Unit,
    onOpenShayariReader: (Shayari) -> Unit,
    onNavigateBack: () -> Unit
) {
    val strings = LocalStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text(strings.publicLibraryTitle) }
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
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; selectedCategory = null },
                        text = { Text(strings.publicBooksTab) },
                        modifier = Modifier.testTag("public_books_tab")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; selectedCategory = null },
                        text = { Text(strings.publicShayariTab) },
                        modifier = Modifier.testTag("public_shayari_tab")
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchPublicHint) },
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
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .testTag("public_search_input")
                )

                val categories = if (selectedTab == 0) KalamoraCatalog.bookCategories else KalamoraCatalog.shayariCategories
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(strings.allCategories) }
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat) }
                        )
                    }
                }

                val q = searchQuery.trim().lowercase()
                if (selectedTab == 0) {
                    when (publicBooksState) {
                        UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        is UiState.Error -> Box(Modifier.padding(20.dp)) {
                            KalamoraErrorBanner(message = publicBooksState.message)
                        }
                        is UiState.Success -> {
                            val itemsList = publicBooksState.data.filter { book ->
                                book.isPublicBook() &&
                                    (selectedCategory == null || book.category == selectedCategory) &&
                                    (q.isEmpty() ||
                                        book.title.lowercase().contains(q) ||
                                        book.authorName.lowercase().contains(q) ||
                                        book.description.lowercase().contains(q) ||
                                        book.category.lowercase().contains(q))
                            }
                            if (itemsList.isEmpty()) {
                                KalamoraEmptyState(
                                    illustrationRes = R.drawable.img_empty_books_1795446303800,
                                    title = strings.emptyPublicTitle,
                                    body = strings.emptyPublicBody
                                )
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(itemsList, key = { it.id }) { book ->
                                        PublicBookCard(book = book, onClick = { onOpenBookReader(book) })
                                    }
                                    item { Spacer(Modifier.height(24.dp)) }
                                }
                            }
                        }
                    }
                } else {
                    when (publicShayariState) {
                        UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        is UiState.Error -> Box(Modifier.padding(20.dp)) {
                            KalamoraErrorBanner(message = publicShayariState.message)
                        }
                        is UiState.Success -> {
                            val itemsList = publicShayariState.data.filter { item ->
                                item.isPublicShayari() &&
                                    (selectedCategory == null || item.category == selectedCategory) &&
                                    (q.isEmpty() ||
                                        item.title.lowercase().contains(q) ||
                                        item.authorName.lowercase().contains(q) ||
                                        item.content.lowercase().contains(q) ||
                                        item.category.lowercase().contains(q))
                            }
                            if (itemsList.isEmpty()) {
                                KalamoraEmptyState(
                                    illustrationRes = R.drawable.img_empty_shayari_1795446302738,
                                    title = strings.emptyPublicTitle,
                                    body = strings.emptyPublicBody
                                )
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(itemsList, key = { it.id }) { shayari ->
                                        PublicShayariCard(shayari = shayari, onClick = { onOpenShayariReader(shayari) })
                                    }
                                    item { Spacer(Modifier.height(24.dp)) }
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
private fun PublicBookCard(book: Book, onClick: () -> Unit) {
    val strings = LocalStrings.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("public_book_${book.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            BookCoverArt(
                title = book.title,
                authorName = book.authorName,
                coverTheme = book.coverTheme,
                coverURL = book.coverURL,
                modifier = Modifier.width(72.dp).height(104.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = literaryFontFor(book.title, book.language)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${strings.byAuthorPrefix} ${book.authorName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (book.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = book.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VisibilityBadge(isPublic = true)
                    Text(
                        text = "${book.category} • ${book.chapterCount} ${strings.chaptersLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PublicShayariCard(shayari: Shayari, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val poeticFont = literaryFontFor(shayari.content, shayari.language)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("public_shayari_${shayari.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = shayari.title,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = poeticFont),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${strings.byAuthorPrefix} ${shayari.authorName} • ${shayari.category}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = shayari.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = poeticFont,
                        lineHeight = 24.sp
                    ),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${strings.publishedOn}: ${DateFormatUtil.formatShort(shayari.publishedAt ?: shayari.updatedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
