package com.example.tv_app.ui.theme

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
        primary = JetStreamPrimary,
        onPrimary = JetStreamOnPrimary,
        primaryContainer = JetStreamPrimaryContainer,
        onPrimaryContainer = JetStreamOnPrimaryContainer,
        secondary = JetStreamSecondary,
        onSecondary = JetStreamOnSecondary,
        secondaryContainer = JetStreamSecondaryContainer,
        onSecondaryContainer = JetStreamOnSecondaryContainer,
        tertiary = JetStreamTertiary,
        onTertiary = JetStreamOnTertiary,
        tertiaryContainer = JetStreamTertiaryContainer,
        onTertiaryContainer = JetStreamOnTertiaryContainer,
        background = JetStreamBackground,
        onBackground = JetStreamOnBackground,
        surface = JetStreamSurface,
        onSurface = JetStreamOnSurface,
        surfaceVariant = JetStreamSurfaceVariant,
        onSurfaceVariant = JetStreamOnSurfaceVariant,
        error = JetStreamError,
        onError = JetStreamOnError,
        errorContainer = JetStreamErrorContainer,
        onErrorContainer = JetStreamOnErrorContainer,
        border = JetStreamBorder
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}