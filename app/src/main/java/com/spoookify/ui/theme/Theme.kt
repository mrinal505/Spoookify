package com.spoookify.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppTheme(
    val id: String, 
    val title: String, 
    val primary: Color, 
    val secondary: Color,
    val background: Color, 
    val surface: Color,
    val cardSurface: Color,
    val topGradientStart: Color,
    val topGradientEnd: Color
) {
    SPOTIFY_GREEN(
        "spotify_green", "Spotify Green", Color(0xFF1DB954), Color(0xFF1ED760), 
        Color(0xFF0D0F14), Color(0xFF161922), Color(0xFF1A1E29), 
        Color(0xFF7000FF).copy(alpha = 0.20f), Color(0xFF1DB954).copy(alpha = 0.10f)
    ),
    AMOLED_PURE_BLACK(
        "amoled_black", "AMOLED Pitch Black", Color(0xFF00E676), Color(0xFF69F0AE), 
        Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFF121212), 
        Color(0xFF00E676).copy(alpha = 0.15f), Color.Transparent
    ),
    CYBERPUNK_NEON(
        "cyberpunk", "Cyberpunk Neon", Color(0xFF00F2FE), Color(0xFF4FACFE), 
        Color(0xFF0A0E1A), Color(0xFF121829), Color(0xFF1A2238), 
        Color(0xFF7000FF).copy(alpha = 0.30f), Color(0xFF00F2FE).copy(alpha = 0.15f)
    ),
    SUNSET_RUBY(
        "sunset_ruby", "Sunset Crimson", Color(0xFFFF1744), Color(0xFFFF5252), 
        Color(0xFF16080D), Color(0xFF240E16), Color(0xFF331420), 
        Color(0xFFFF1744).copy(alpha = 0.25f), Color(0xFFFF9100).copy(alpha = 0.12f)
    ),
    OCEAN_DEEP(
        "ocean_deep", "Ocean Deep Teal", Color(0xFF00E5FF), Color(0xFF18FFFF), 
        Color(0xFF07141E), Color(0xFF0E2231), Color(0xFF153144), 
        Color(0xFF00E5FF).copy(alpha = 0.25f), Color(0xFF00BFA5).copy(alpha = 0.12f)
    ),
    EMERALD_FOREST(
        "emerald_forest", "Emerald Forest", Color(0xFF00E676), Color(0xFF1DE9B6), 
        Color(0xFF091A14), Color(0xFF10281F), Color(0xFF183B2D), 
        Color(0xFF00E676).copy(alpha = 0.22f), Color(0xFF00B0FF).copy(alpha = 0.10f)
    ),
    ROYAL_AMETHYST(
        "royal_amethyst", "Royal Amethyst", Color(0xFFD500F9), Color(0xFFE040FB), 
        Color(0xFF14081E), Color(0xFF220E30), Color(0xFF301445), 
        Color(0xFFD500F9).copy(alpha = 0.28f), Color(0xFF651FFF).copy(alpha = 0.12f)
    ),
    SOLAR_FLARE(
        "solar_flare", "Solar Gold", Color(0xFFFFC400), Color(0xFFFFD740), 
        Color(0xFF141008), Color(0xFF241C0E), Color(0xFF352914), 
        Color(0xFFFFC400).copy(alpha = 0.25f), Color(0xFFFF3D00).copy(alpha = 0.12f)
    )
}

val LocalAppScale = compositionLocalOf { 1.0f }
val LocalAppTheme = compositionLocalOf { AppTheme.SPOTIFY_GREEN }

@Composable
fun SpoookifyTheme(
    appTheme: AppTheme = AppTheme.SPOTIFY_GREEN,
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoledBlack: Boolean = false,
    useDynamicColors: Boolean = false,
    scaleFactor: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamic = dynamicDarkColorScheme(context)
            if (isAmoledBlack) dynamic.copy(background = Color.Black, surface = Color.Black) else dynamic
        }
        else -> {
            darkColorScheme(
                primary = appTheme.primary,
                secondary = appTheme.secondary,
                onPrimary = Color.Black,
                background = if (isAmoledBlack) Color.Black else appTheme.background,
                onBackground = White,
                surface = if (isAmoledBlack) Color.Black else appTheme.surface,
                onSurface = White
            )
        }
    }

    CompositionLocalProvider(
        LocalAppScale provides scaleFactor,
        LocalAppTheme provides appTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
