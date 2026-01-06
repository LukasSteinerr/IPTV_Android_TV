package com.example.iptvsonic.mobile_ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.launch

@Composable
fun PaletteBackedContent(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(Color) -> Unit
) {
    val context = LocalContext.current
    var backgroundColor by remember { mutableStateOf(Color.Black) }
    val animatedBackgroundColor by animateColorAsState(
        targetValue = backgroundColor,
        label = "BackgroundColor"
    )

    LaunchedEffect(imageUrl) {
        if (imageUrl != null) {
            launch {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()

                val result = loader.execute(request)
                if (result is SuccessResult) {
                    try {
                        val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                            ?: result.drawable.toBitmap()

                        androidx.palette.graphics.Palette.from(bitmap).generate { palette ->
                            val swatch = palette?.vibrantSwatch ?: palette?.darkVibrantSwatch ?: palette?.dominantSwatch
                            swatch?.let {
                                backgroundColor = Color(it.rgb).copy(alpha = 0.6f)
                            }
                        }
                    } catch (e: Exception) {
                        // Fallback to black if conversion fails
                        backgroundColor = Color.Black
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        animatedBackgroundColor,
                        Color.Transparent
                    )
                )
            )
            .padding(vertical = 16.dp)
    ) {
        content(animatedBackgroundColor)
    }
}