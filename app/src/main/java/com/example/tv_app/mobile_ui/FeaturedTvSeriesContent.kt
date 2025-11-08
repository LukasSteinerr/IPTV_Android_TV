package com.example.tv_app.mobile_ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import com.example.tv_app.model.TvSeries
import com.example.tv_app.repository.TMDBImageProvider
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable

@Composable
fun FeaturedTvSeriesContent(
    tvSeries: List<TvSeries>,
    onPlayTapped: (TvSeries) -> Unit,
    onDetailsTapped: (TvSeries) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tvSeries.isEmpty()) {
        return
    }

    // For mobile, display only the first featured series in a large card format
    val series = tvSeries.first()
    val tmdbImageProvider = remember { TMDBImageProvider.getInstance() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp) // Reduced height for mobile
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onDetailsTapped(series) }
    ) {
        // background
        FeaturedItemBackground(
            tvSeries = series,
            tmdbImageProvider = tmdbImageProvider,
            modifier = Modifier.fillMaxSize()
        )
        // foreground
        FeaturedItemForeground(
            tvSeries = series,
            onPlayTapped = { onPlayTapped(series) },
            onDetailsTapped = { onDetailsTapped(series) },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun FeaturedItemForeground(
    tvSeries: TvSeries,
    onPlayTapped: () -> Unit,
    onDetailsTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomStart
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Reduced padding for mobile
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = tvSeries.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(x = 1f, y = 2f),
                        blurRadius = 1f
                    )
                ),
                color = Color.White,
                maxLines = 1
            )
            
            val description = tvSeries.description
            if (!description.isNullOrEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.8f
                        ),
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(x = 1f, y = 2f),
                            blurRadius = 1f
                        )
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Play button is always visible on mobile featured content
            WatchNowButton(onPlayTapped = onPlayTapped)
        }
    }
}

@Composable
private fun FeaturedItemBackground(
    tvSeries: TvSeries,
    tmdbImageProvider: TMDBImageProvider,
    modifier: Modifier = Modifier
) {
    var imageUrl by remember { mutableStateOf<String?>(null) }
    
    // Fetch the best available image URL for this TV series
    LaunchedEffect(tvSeries.tmdbId) {
        imageUrl = tmdbImageProvider.getBackdropUrl(tvSeries.tmdbId)
            ?: tmdbImageProvider.getPosterUrl(tvSeries.tmdbId, tvSeries.coverUrl)
    }

    AsyncImage(
        model = imageUrl,
        contentDescription = tvSeries.name,
        modifier = modifier
            .drawWithContent {
                drawContent()
                // Dark gradient overlay for text readability
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        ),
                        startY = 0f,
                        endY = size.height
                    )
                )
            },
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun WatchNowButton(
    onPlayTapped: () -> Unit
) {
    Button(
        onClick = onPlayTapped,
        modifier = Modifier.padding(top = 16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    ) {
        Icon(
            imageVector = Icons.Outlined.PlayArrow,
            contentDescription = "Play",
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "Watch Now",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Medium
            )
        )
    }
}
