package com.example.iptvsonic.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TV_APPTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = darkColorScheme(
        primary = DarkPrimary,
        onPrimary = DarkOnPrimary,
        secondary = DarkSecondary,
        onSecondary = DarkOnSecondary,
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        error = DarkError,
        onError = DarkOnError,
        // Using DarkBackground for containers to maintain a consistent dark look
        primaryContainer = DarkBackground,
        onPrimaryContainer = DarkOnBackground,
        secondaryContainer = DarkBackground,
        onSecondaryContainer = DarkOnBackground,
        tertiary = DarkPrimary,
        onTertiary = DarkOnPrimary,
        tertiaryContainer = DarkBackground,
        onTertiaryContainer = DarkOnBackground,
        surfaceVariant = DarkSurface,
        onSurfaceVariant = DarkOnSurface
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
