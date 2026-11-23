package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Book
import com.example.data.model.DashboardStats
import com.example.data.model.RecentWorkItem
import com.example.data.model.Shayari
import com.example.ui.components.BookCoverArt
import com.example.ui.components.DateFormatUtil
import com.example.ui.components.VisibilityBadge
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.DeepIndigoDark
import com.example.ui.theme.MutedGold
import com.example.ui.theme.literaryFontFor

@Composable
fun HomeDashboardScreen(
    authorDisplayName: String,
    stats: DashboardStats,
    recentWorks: List<RecentWorkItem>,
    publicBooksCount: Int,
    publicShayariCount: Int,
    onCreateNewBook: () -> Unit,
    onCreateNewShayari: () -> Unit,
    onOpenBookStudio: (Book) -> Unit,
    onOpenShayariStudio: (Shayari) -> Unit,
    onOpenPublicLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val firstName = authorDisplayName.trim().substringBefore(" ").ifBlank { "Writer" }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Literary Greeting Hero Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(DeepIndigo, DeepIndigoDark)
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column {
                            Text(
                                text = "KALAMORA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedGold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${strings.welcomeBackPrefix} $firstName!",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color(0xFFFCFBF8)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "\"${strings.dailyQuote}\"",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic,
                                    fontFamily = literaryFontFor(strings.dailyQuote)
                                ),
                                color = Color(0xFFE7DFCF)
                            )
                        }
                    }
                }
            }

            // Two Prominent Creation Cards: Write a Book & Write Shayari
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StudioActionCard(
                        title = strings.writeABook,
                        subtitle = strings.writeABookSubtitle,
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = onCreateNewBook,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_create_book_card")
                    )
                    StudioActionCard(
                        title = strings.writeShayari,
                        subtitle = strings.writeShayariSubtitle,
                        icon = Icons.Filled.EditNote,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = onCreateNewShayari,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_create_shayari_card")
                    )
                }
            }

            // Real-Time Firestore Statistics Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatTile(
                            label = strings.statBooks,
                            value = stats.totalBooks.toString(),
                            icon = Icons.Filled.LibraryBooks,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = strings.statShayari,
                            value = stats.totalShayari.toString(),
                            icon = Icons.Filled.AutoStories,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = strings.statWords,
                            value = formatCount(stats.totalWords),
                            icon = Icons.Filled.TextFields,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatTile(
                            label = strings.statDrafts,
                            value = stats.totalDrafts.toString(),
                            icon = Icons.Filled.Lock,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = strings.statPublished,
                            value = stats.totalPublished.toString(),
                            icon = Icons.Filled.Public,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Public Reading Library Gateway
            item {
                Card(
                    onClick = onOpenPublicLibrary,
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_public_library_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.explorePublicLibrary,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${strings.explorePublicLibraryDesc} ($publicBooksCount ${strings.publicBooksTab} • $publicShayariCount ${strings.publicShayariTab})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = strings.explorePublicLibrary,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Recently Edited Section Header
            item {
                Text(
                    text = strings.recentlyEdited,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (recentWorks.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.noRecentWork,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(recentWorks, key = { "${it::class.simpleName}_${it.id}" }) { work ->
                    RecentWorkCard(
                        work = work,
                        onClick = {
                            when (work) {
                                is RecentWorkItem.BookItem -> onOpenBookStudio(work.book)
                                is RecentWorkItem.ShayariItem -> onOpenShayariStudio(work.shayari)
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun StudioActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentWorkCard(
    work: RecentWorkItem,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_work_${work.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (work) {
                is RecentWorkItem.BookItem -> {
                    BookCoverArt(
                        title = work.book.title,
                        authorName = work.book.authorName,
                        coverTheme = work.book.coverTheme,
                        coverURL = work.book.coverURL,
                        compact = true,
                        modifier = Modifier
                            .width(48.dp)
                            .height(66.dp)
                    )
                }
                is RecentWorkItem.ShayariItem -> {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = work.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = literaryFontFor(work.title)
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${work.subtitle} • ${work.wordCount} ${strings.wordsLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VisibilityBadge(isPublic = work.isPublic)
                    Text(
                        text = DateFormatUtil.formatDateTime(work.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatCount(value: Long): String = when {
    value >= 1_000_000L -> String.format("%.1fM", value / 1_000_000.0)
    value >= 10_000L -> String.format("%.1fk", value / 1_000.0)
    else -> value.toString()
}
