package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Kalamora Brand Identity Palette
val DeepIndigo = Color(0xFF302B63)
val DeepIndigoDark = Color(0xFF1F1B45)
val DeepIndigoLight = Color(0xFF4B458C)
val WarmCream = Color(0xFFF7F3EB)
val SoftParchment = Color(0xFFEDE6D8)
val MutedGold = Color(0xFFC6A15B)
val BurnishedGold = Color(0xFF9F7A36)
val PaleGold = Color(0xFFF0E2C4)
val DarkCharcoal = Color(0xFF252525)
val MutedInk = Color(0xFF5D5954)
val SoftOffWhite = Color(0xFFFCFBF8)
val PurePaper = Color(0xFFFFFFFF)
val SubtleBorder = Color(0xFFE5DFD3)
val ForestPublished = Color(0xFF2E6B56)
val ForestPublishedContainer = Color(0xFFE3F2EC)
val CrimsonError = Color(0xFFB3261E)
val CrimsonErrorContainer = Color(0xFFF9DEDC)

// Dark Mode Literary Study Palette
val DarkMidnightBg = Color(0xFF12111B)
val DarkStudySurface = Color(0xFF1B1928)
val DarkElevatedSurface = Color(0xFF242236)
val DarkSurfaceVariant = Color(0xFF2D2A42)
val DarkIndigoPrimary = Color(0xFFB8B2F0)
val DarkOnPrimary = Color(0xFF1D1847)
val DarkPrimaryContainer = Color(0xFF3B3578)
val DarkGoldSecondary = Color(0xFFD8B878)
val DarkOnSecondary = Color(0xFF2F2309)
val DarkSecondaryContainer = Color(0xFF4A3A18)
val DarkWarmIvory = Color(0xFFF2EEE6)
val DarkMutedText = Color(0xFFB9B3A8)
val DarkOutline = Color(0xFF423F57)
val DarkPublished = Color(0xFF7FD1B2)
val DarkPublishedContainer = Color(0xFF1A3A2E)

data class BookCoverPalette(
    val key: String,
    val labelEn: String,
    val labelHi: String,
    val background: Color,
    val accent: Color,
    val textColor: Color
)

val KalamoraCoverPalettes = listOf(
    BookCoverPalette("indigo_gold", "Royal Indigo", "शाही नील", Color(0xFF302B63), Color(0xFFC6A15B), Color(0xFFF7F3EB)),
    BookCoverPalette("cream_ink", "Warm Parchment", "गर्म कागज़", Color(0xFFF2E9D8), Color(0xFF302B63), Color(0xFF252525)),
    BookCoverPalette("forest_linen", "Emerald Linen", "पन्ना हरा", Color(0xFF1E4337), Color(0xFFD7B574), Color(0xFFF7F3EB)),
    BookCoverPalette("crimson_velvet", "Burgundy Folio", "बरगंडी जिल्द", Color(0xFF5D2130), Color(0xFFE3C282), Color(0xFFFBF6EE)),
    BookCoverPalette("midnight_quill", "Midnight Ink", "मध्यरात्रि स्याही", Color(0xFF1A2233), Color(0xFFC6A15B), Color(0xFFF5F1E8)),
    BookCoverPalette("ochre_paper", "Terracotta Clay", "टेराकोटा", Color(0xFF7A4428), Color(0xFFF3DEC0), Color(0xFFFFFAF2))
)

fun findCoverPalette(key: String): BookCoverPalette =
    KalamoraCoverPalettes.firstOrNull { it.key == key } ?: KalamoraCoverPalettes.first()
