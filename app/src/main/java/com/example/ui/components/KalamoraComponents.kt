package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SaveStatus
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.findCoverPalette
import com.example.ui.theme.literaryFontFor
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BookCoverArt(
    title: String,
    authorName: String,
    coverTheme: String,
    coverURL: String?,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val palette = findCoverPalette(coverTheme)
    val decodedBitmap = rememberDecodedDataUri(coverURL)
    val hasRemoteUrl = !coverURL.isNullOrBlank() && !ImageCoverUtil.isDataUri(coverURL)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.background)
            .border(
                width = 1.dp,
                color = palette.accent.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        when {
            decodedBitmap != null -> {
                Image(
                    bitmap = decodedBitmap,
                    contentDescription = "Book cover for $title",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            hasRemoteUrl -> {
                AsyncImage(
                    model = coverURL,
                    contentDescription = "Book cover for $title",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                // Literary book cover design with subtle spine and gold ornamental rule
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    palette.background,
                                    palette.background.copy(alpha = 0.88f)
                                )
                            )
                        )
                )
                // Spine stripe
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(if (compact) 6.dp else 10.dp)
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.16f))
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = if (compact) 12.dp else 18.dp,
                            end = if (compact) 8.dp else 14.dp,
                            top = if (compact) 10.dp else 16.dp,
                            bottom = if (compact) 10.dp else 14.dp
                        ),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    HorizontalDivider(
                        color = palette.accent.copy(alpha = 0.7f),
                        thickness = 1.dp,
                        modifier = Modifier.width(if (compact) 22.dp else 36.dp)
                    )
                    Text(
                        text = title.ifBlank { "Untitled" },
                        style = (if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium).copy(
                            fontFamily = literaryFontFor(title),
                            color = palette.textColor,
                            lineHeight = if (compact) 16.sp else 21.sp
                        ),
                        maxLines = if (compact) 3 else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = authorName.ifBlank { "Kalamora" },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = palette.accent,
                            fontSize = if (compact) 9.sp else 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun VisibilityBadge(
    isPublic: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val containerColor = if (isPublic) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isPublic) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isPublic) Icons.Filled.Public else Icons.Filled.Lock,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (isPublic) strings.publicBadge else strings.privateDraftBadge,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

@Composable
fun SaveStatusPill(
    status: SaveStatus,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    when (status) {
        SaveStatus.Idle -> Unit
        SaveStatus.Unsaved -> {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = modifier
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = strings.statusUnsaved,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        SaveStatus.Saving -> {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = modifier
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.statusSaving,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        is SaveStatus.Saved -> {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = modifier
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = strings.statusSaved,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
        is SaveStatus.Error -> {
            Surface(
                onClick = { onRetry?.invoke() },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = modifier
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${strings.statusSaveFailed} • ${strings.retryLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun KalamoraEmptyState(
    @DrawableRes illustrationRes: Int,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    imageHeight: Dp = 170.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = illustrationRes),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(230.dp)
                .height(imageHeight)
                .clip(RoundedCornerShape(20.dp))
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
fun KalamoraErrorBanner(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text(
                        text = strings.retryLabel,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

object DateFormatUtil {
    private val shortDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    fun formatShort(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: return "Just now"
        return shortDateFormat.format(date)
    }

    fun formatDateTime(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: return "Just now"
        return dateTimeFormat.format(date)
    }

    fun formatMillis(ms: Long): String = dateTimeFormat.format(Date(ms))
}

object ShareUtil {
    fun shareText(context: Context, title: String, body: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(intent, "Share via Kalamora"))
    }
}
