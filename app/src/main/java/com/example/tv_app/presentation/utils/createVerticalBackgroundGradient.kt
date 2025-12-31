package com.example.tv_app.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.tv_app.model.MoviePalette

@Composable
fun createVerticalBackgroundGradient(palette: MoviePalette): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            palette.background.copy(alpha = 0.8f),
            Color.Black
        )
    )
}