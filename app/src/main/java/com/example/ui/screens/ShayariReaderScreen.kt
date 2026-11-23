package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Shayari
import com.example.data.model.UiState
import com.example.ui.components.DateFormatUtil
import com.example.ui.components.KalamoraErrorBanner
import com.example.ui.components.ShareUtil
import com.example.ui.components.VisibilityBadge
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.literaryFontFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShayariReaderScreen(
    shayariState: UiState<Shayari?>,
    onNavigateBack: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val shayari = (shayariState as? UiState.Success)?.data

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text(strings.shayariStudioTitle) },
                actions = {
                    if (shayari != null) {
                        IconButton(
                            onClick = {
                                ShareUtil.shareText(
                                    context = context,
                                    title = shayari.title,
                                    body = "${shayari.title}\n\n${shayari.content}\n\n— ${shayari.authorName}\nRead on Kalamora: https://kalamora.app/shayari/${shayari.id}"
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
        when (shayariState) {
            UiState.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is UiState.Error -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                KalamoraErrorBanner(message = shayariState.message)
            }
            is UiState.Success -> {
                if (shayari == null) {
                    Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text("This shayari is private or unavailable.")
                    }
                } else {
                    val poeticFont = literaryFontFor(shayari.content, shayari.language)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 640.dp)
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 26.dp, vertical = 34.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    VisibilityBadge(isPublic = shayari.isPublicShayari())
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = shayari.title,
                                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = poeticFont),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "${shayari.category} • ${shayari.language}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(18.dp))
                                    HorizontalDivider(
                                        modifier = Modifier.width(72.dp),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(Modifier.height(24.dp))

                                    // Preserves exact poetic line breaks and paragraph spacing
                                    Text(
                                        text = shayari.content,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = poeticFont,
                                            fontSize = 19.sp,
                                            lineHeight = 34.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(Modifier.height(28.dp))
                                    HorizontalDivider(
                                        modifier = Modifier.width(48.dp),
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = "${strings.byAuthorPrefix} ${shayari.authorName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "${strings.publishedOn}: ${DateFormatUtil.formatShort(shayari.publishedAt ?: shayari.updatedAt)}",
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
}
