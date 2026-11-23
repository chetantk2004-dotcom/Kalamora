package com.example.ui.theme

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val KalamoraLightColorScheme = lightColorScheme(
    primary = DeepIndigo,
    onPrimary = SoftOffWhite,
    primaryContainer = WarmCream,
    onPrimaryContainer = DeepIndigoDark,
    secondary = MutedGold,
    onSecondary = DarkCharcoal,
    secondaryContainer = PaleGold,
    onSecondaryContainer = BurnishedGold,
    tertiary = ForestPublished,
    onTertiary = PurePaper,
    tertiaryContainer = ForestPublishedContainer,
    onTertiaryContainer = ForestPublished,
    background = SoftOffWhite,
    onBackground = DarkCharcoal,
    surface = PurePaper,
    onSurface = DarkCharcoal,
    surfaceVariant = WarmCream,
    onSurfaceVariant = MutedInk,
    surfaceContainerLowest = PurePaper,
    surfaceContainerLow = SoftOffWhite,
    surfaceContainer = WarmCream,
    surfaceContainerHigh = SoftParchment,
    outline = SubtleBorder,
    outlineVariant = SoftParchment,
    error = CrimsonError,
    onError = PurePaper,
    errorContainer = CrimsonErrorContainer,
    onErrorContainer = CrimsonError
)

private val KalamoraDarkColorScheme = darkColorScheme(
    primary = DarkIndigoPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkWarmIvory,
    secondary = DarkGoldSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkWarmIvory,
    tertiary = DarkPublished,
    onTertiary = DarkMidnightBg,
    tertiaryContainer = DarkPublishedContainer,
    onTertiaryContainer = DarkPublished,
    background = DarkMidnightBg,
    onBackground = DarkWarmIvory,
    surface = DarkStudySurface,
    onSurface = DarkWarmIvory,
    surfaceVariant = DarkElevatedSurface,
    onSurfaceVariant = DarkMutedText,
    surfaceContainerLowest = DarkMidnightBg,
    surfaceContainerLow = DarkStudySurface,
    surfaceContainer = DarkElevatedSurface,
    surfaceContainerHigh = DarkSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkSurfaceVariant,
    error = CrimsonErrorContainer,
    onError = CrimsonError,
    errorContainer = CrimsonError,
    onErrorContainer = CrimsonErrorContainer
)

val KalamoraShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KalamoraTheme(
    themeMode: String = "system",
    interfaceLanguage: String = "en",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    DisposableEffect(context, darkTheme) {
        val activity = context as? ComponentActivity
        activity?.enableEdgeToEdge(
            statusBarStyle = if (darkTheme) {
                SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            } else {
                SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            },
            navigationBarStyle = if (darkTheme) {
                SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            } else {
                SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            }
        )
        onDispose {}
    }

    val colorScheme = if (darkTheme) KalamoraDarkColorScheme else KalamoraLightColorScheme
    val typography = remember(interfaceLanguage) { buildKalamoraTypography(interfaceLanguage) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = KalamoraShapes,
        content = content
    )
}
