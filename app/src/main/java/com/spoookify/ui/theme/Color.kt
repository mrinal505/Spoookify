package com.spoookify.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val SpotifyGreen = Color(0xFF1DB954)
val SpotifyBlack = Color(0xFF0D0F14)
val AmoledBlack = Color(0xFF050608)
val SpotifyDarkGrey = Color(0xFF161922)
val SpotifyLightGrey = Color(0xFF232733)
val White = Color(0xFFFFFFFF)

// Glassmorphism & Neon Palette
val NeonCyan = Color(0xFF00F2FE)
val ElectricPurple = Color(0xFF7000FF)
val BrightPink = Color(0xFFFF2A85)
val WarmGold = Color(0xFFFFB300)

val GlassBackground = Color(0x1FFFFFFF)
val GlassBorder = Color(0x33FFFFFF)
val GlassSurface = Color(0x14FFFFFF)
val CardSurface = Color(0xFF12151E)
val CardBorder = Color(0x1F455A64)

val AccentGradient = Brush.horizontalGradient(
    colors = listOf(SpotifyGreen, NeonCyan)
)

val HeroGradient = Brush.verticalGradient(
    colors = listOf(ElectricPurple.copy(alpha = 0.35f), SpotifyGreen.copy(alpha = 0.15f), Color.Transparent)
)

