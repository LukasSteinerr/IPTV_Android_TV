package com.example.iptvsonic.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Netflix-style dark grey color (similar to Colors.grey[900] in Flutter)
private val NetflixDarkGray = Color(0xFF212121)

/**
 * A Netflix-style loading indicator that uses a solid dark gray background
 * as a skeleton screen during image loading.
 */
@Composable
fun NetflixStyleLoading(
    modifier: Modifier = Modifier,
    borderRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .background(
                color = NetflixDarkGray,
                shape = RoundedCornerShape(borderRadius)
            )
    )
}

/**
 * A fallback placeholder used when image loading fails or a URL is missing.
 * Displays only a solid dark gray background.
 */
@Composable
fun ImageFallbackPlaceholder(
    modifier: Modifier = Modifier,
    borderRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .background(
                color = NetflixDarkGray,
                shape = RoundedCornerShape(borderRadius)
            )
    )
}