package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.DashboardStats
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.DateFormatUtil
import com.example.ui.components.ImageCoverUtil
import com.example.ui.components.KalamoraErrorBanner
import com.example.ui.components.rememberDecodedDataUri
import com.example.ui.i18n.LocalStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileSettingsScreen(
    profileState: UiState<UserProfile?>,
    stats: DashboardStats,
    interfaceLanguage: String,
    themeMode: String,
    isDeletingAccount: Boolean,
    onSaveProfile: (
        displayName: String,
        username: String,
        bio: String?,
        photoURL: String?,
        preferredLanguage: String,
        darkMode: Boolean
    ) -> Unit,
    onChangeInterfaceLanguage: (String) -> Unit,
    onChangeThemeMode: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val profile = (profileState as? UiState.Success)?.data
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var photoURL by remember { mutableStateOf<String?>(null) }
    var hydratedUid by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(profile?.uid, profile?.displayName, profile?.username, profile?.bio, profile?.photoURL) {
        if (profile != null && hydratedUid != profile.uid) {
            hydratedUid = profile.uid
            displayName = profile.displayName
            username = profile.username
            bio = profile.bio ?: ""
            photoURL = profile.photoURL
        }
    }

    // Zero-permission Photo Picker for profile avatar
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val compressed = withContext(Dispatchers.IO) {
                    ImageCoverUtil.uriToCompactDataUri(context, uri)
                }
                if (compressed != null) {
                    photoURL = compressed
                }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (profileState is UiState.Error) {
                KalamoraErrorBanner(message = profileState.message)
            }

            // Author Identity Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val decodedAvatar = rememberDecodedDataUri(photoURL)
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    avatarPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                decodedAvatar != null -> {
                                    Image(
                                        bitmap = decodedAvatar,
                                        contentDescription = "Author profile photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                !photoURL.isNullOrBlank() && !ImageCoverUtil.isDataUri(photoURL) -> {
                                    AsyncImage(
                                        model = photoURL,
                                        contentDescription = "Author profile photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Author avatar placeholder",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = "Change avatar",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.displayName?.ifBlank { "Kalamora Author" } ?: "Kalamora Author",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "@${profile?.username ?: "writer"}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (!profile?.email.isNullOrBlank()) {
                                Text(
                                    text = profile?.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Joined ${DateFormatUtil.formatShort(profile?.createdAt)} • ${stats.totalBooks} ${strings.statBooks} • ${stats.totalShayari} ${strings.statShayari}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { if (it.length <= 100) displayName = it; validationError = null },
                        label = { Text(strings.displayNameLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_display_name_input")
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { if (it.length <= 40) username = it; validationError = null },
                        label = { Text(strings.usernameLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_username_input")
                    )

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { if (it.length <= 300) bio = it },
                        label = { Text(strings.bioLabel) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (validationError != null) {
                        Text(
                            text = validationError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            when {
                                displayName.trim().isEmpty() -> validationError = "Display name cannot be empty."
                                username.trim().length < 2 -> validationError = "Username must be at least 2 characters."
                                else -> onSaveProfile(
                                    displayName.trim(),
                                    username.trim(),
                                    bio.trim().ifBlank { null },
                                    photoURL,
                                    interfaceLanguage,
                                    themeMode == "dark"
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_profile_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.saveLabel)
                    }
                }
            }

            // Interface Language Settings
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Language, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(strings.interfaceLanguageLabel, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = strings.interfaceLanguageNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = interfaceLanguage == "en",
                            onClick = { onChangeInterfaceLanguage("en") },
                            label = { Text("English") },
                            modifier = Modifier.testTag("settings_lang_en")
                        )
                        FilterChip(
                            selected = interfaceLanguage == "hi",
                            onClick = { onChangeInterfaceLanguage("hi") },
                            label = { Text("हिन्दी (Hindi)") },
                            modifier = Modifier.testTag("settings_lang_hi")
                        )
                    }
                }
            }

            // Appearance / Light & Dark Mode
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Palette, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(strings.appearanceLabel, style = MaterialTheme.typography.titleMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = themeMode == "system",
                            onClick = { onChangeThemeMode("system") },
                            label = { Text(strings.themeSystem) }
                        )
                        FilterChip(
                            selected = themeMode == "light",
                            onClick = { onChangeThemeMode("light") },
                            label = { Text(strings.themeLight) },
                            modifier = Modifier.testTag("settings_theme_light")
                        )
                        FilterChip(
                            selected = themeMode == "dark",
                            onClick = { onChangeThemeMode("dark") },
                            label = { Text(strings.themeDark) },
                            modifier = Modifier.testTag("settings_theme_dark")
                        )
                    }
                }
            }

            // Sign Out & Account Deletion
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(strings.accountSettings, style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.signOutLabel)
                    }

                    Button(
                        onClick = { showDeleteAccountDialog = true },
                        enabled = !isDeletingAccount,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("delete_account_button")
                    ) {
                        if (isDeletingAccount) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.DeleteForever, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.deleteAccountLabel)
                        }
                    }
                }
            }

            Spacer(Modifier.height(72.dp))
        }
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text(strings.deleteAccountTitle) },
            text = { Text(strings.deleteAccountBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccount()
                    }
                ) {
                    Text(strings.deleteAccountConfirmAction, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(strings.cancelLabel)
                }
            }
        )
    }
}
