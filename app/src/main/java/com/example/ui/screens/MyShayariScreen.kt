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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.KalamoraCatalog
import com.example.data.model.LibraryFilter
import com.example.data.model.Shayari
import com.example.data.model.SortOption
import com.example.data.model.UiState
import com.example.ui.components.DateFormatUtil
import com.example.ui.components.KalamoraEmptyState
import com.example.ui.components.KalamoraErrorBanner
import com.example.ui.components.VisibilityBadge
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.literaryFontFor
import com.google.firebase.Timestamp

@Composable
fun MyShayariScreen(
    shayariState: UiState<List<Shayari>>,
    onCreateNewShayari: () -> Unit,
    onOpenShayariStudio: (Shayari) -> Unit,
    onPreviewShayari: (Shayari) -> Unit,
    onToggleShayariVisibility: (Shayari, Boolean) -> Unit,
    onDeleteShayari: (Shayari) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(SortOption.RECENTLY_EDITED) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    var pendingDelete by remember { mutableStateOf<Shayari?>(null) }
    var pendingVisibility by remember { mutableStateOf<Pair<Shayari, Boolean>?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
        ) {
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
                    placeholder = { Text(strings.searchShayariHint) },
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
                        .testTag("shayari_search_input")
                )

                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier.testTag("shayari_sort_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort shayari")
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
                items(KalamoraCatalog.shayariCategories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat) }
                    )
                }
            }

            when (shayariState) {
                UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.padding(20.dp)) {
                        KalamoraErrorBanner(message = shayariState.message)
                    }
                }
                is UiState.Success -> {
                    val query = searchQuery.trim().lowercase()
                    val filtered = remember(shayariState.data, query, activeFilter, selectedCategory, sortOption) {
                        val base = shayariState.data.filter { item ->
                            val matchesStatus = when (activeFilter) {
                                LibraryFilter.ALL -> true
                                LibraryFilter.DRAFTS -> !item.isPublicShayari()
                                LibraryFilter.PUBLISHED -> item.isPublicShayari()
                                LibraryFilter.RECENT -> true
                            }
                            val matchesCat = selectedCategory == null || item.category == selectedCategory
                            val matchesQuery = query.isEmpty() ||
                                item.title.lowercase().contains(query) ||
                                item.content.lowercase().contains(query) ||
                                item.category.lowercase().contains(query) ||
                                item.language.lowercase().contains(query)
                            matchesStatus && matchesCat && matchesQuery
                        }
                        val sorted = when (sortOption) {
                            SortOption.RECENTLY_EDITED -> base.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                            SortOption.TITLE_AZ -> base.sortedBy { it.title.lowercase() }
                            SortOption.NEWEST -> base.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        }
                        if (activeFilter == LibraryFilter.RECENT) sorted.take(8) else sorted
                    }

                    if (filtered.isEmpty()) {
                        KalamoraEmptyState(
                            illustrationRes = R.drawable.img_empty_shayari_1795446302738,
                            title = strings.emptyShayariTitle,
                            body = strings.emptyShayariBody,
                            actionLabel = if (shayariState.data.isEmpty()) strings.writeShayari else null,
                            onAction = if (shayariState.data.isEmpty()) onCreateNewShayari else null
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filtered, key = { it.id }) { item ->
                                ShayariLibraryCard(
                                    shayari = item,
                                    onOpen = { onOpenShayariStudio(item) },
                                    onPreview = { onPreviewShayari(item) },
                                    onToggleVisibility = { makePublic ->
                                        pendingVisibility = Pair(item, makePublic)
                                    },
                                    onDelete = { pendingDelete = item }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(76.dp)) }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(strings.deleteShayariTitle) },
            text = { Text("${item.title}\n\n${strings.deleteShayariBody}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteShayari(item)
                        pendingDelete = null
                    }
                ) {
                    Text(strings.deleteLabel, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(strings.cancelLabel)
                }
            }
        )
    }

    pendingVisibility?.let { (item, makePublic) ->
        AlertDialog(
            onDismissRequest = { pendingVisibility = null },
            title = { Text(if (makePublic) strings.publishConfirmTitle else strings.unpublishConfirmTitle) },
            text = { Text(if (makePublic) strings.publishConfirmBody else strings.unpublishConfirmBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onToggleShayariVisibility(item, makePublic)
                        pendingVisibility = null
                    }
                ) {
                    Text(if (makePublic) strings.publishPublicly else strings.makePrivate)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingVisibility = null }) {
                    Text(strings.cancelLabel)
                }
            }
        )
    }
}

@Composable
private fun ShayariLibraryCard(
    shayari: Shayari,
    onOpen: () -> Unit,
    onPreview: () -> Unit,
    onToggleVisibility: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalStrings.current
    var menuOpen by remember { mutableStateOf(false) }
    val poeticFont = literaryFontFor(shayari.content, shayari.language)

    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shayari_card_${shayari.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = shayari.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = poeticFont),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Shayari options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(strings.previewLabel) },
                            leadingIcon = { Icon(Icons.Filled.Visibility, null) },
                            onClick = { menuOpen = false; onPreview() }
                        )
                        DropdownMenuItem(
                            text = { Text(if (shayari.isPublicShayari()) strings.makePrivate else strings.publishPublicly) },
                            leadingIcon = {
                                Icon(
                                    if (shayari.isPublicShayari()) Icons.Filled.Lock else Icons.Filled.Public,
                                    null
                                )
                            },
                            onClick = { menuOpen = false; onToggleVisibility(!shayari.isPublicShayari()) }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.deleteLabel, color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; onDelete() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Preserve line breaks verbatim inside card excerpt
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
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VisibilityBadge(isPublic = shayari.isPublicShayari())
                    Text(
                        text = "${shayari.category} • ${shayari.language}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = DateFormatUtil.formatShort(shayari.updatedAt ?: shayari.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
