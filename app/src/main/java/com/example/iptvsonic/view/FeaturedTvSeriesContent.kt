package com.example.iptvsonic.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.example.iptvsonic.model.TvSeries
import com.example.iptvsonic.repository.TMDBImageProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
private val CarouselSaver = Saver<CarouselState, Int>(
    save = { it.activeItemIndex },
    restore = { CarouselState(it) }
)

@OptIn(ExperimentalTvMaterial3Api::class)
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

    val carouselState = rememberSaveable(saver = CarouselSaver) { CarouselState(0) }
    var isCarouselFocused by remember { mutableStateOf(false) }
    val alpha = if (isCarouselFocused) {
        1f
    } else {
        0f
    }

    val tmdbImageProvider = remember { TMDBImageProvider.getInstance() }

    Carousel(
        modifier = modifier
            .fillMaxWidth()
            .height(324.dp)
            .padding(horizontal = 48.dp)
            .border(
                width = if (isCarouselFocused) 3.dp else 2.dp,
                color = if (isCarouselFocused) Color(0xFFE3E2E6) else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
            )
            .clip(RoundedCornerShape(16.dp))
            .onFocusChanged {
                // Because the carousel itself never gets the focus
                isCarouselFocused = it.hasFocus
            },
        itemCount = tvSeries.size,
        carouselState = carouselState,
        carouselIndicator = {
            CarouselIndicator(
                itemCount = tvSeries.size,
                activeItemIndex = carouselState.activeItemIndex
            )
        },
        contentTransformStartToEnd = fadeIn(tween(durationMillis = 1000))
            .togetherWith(fadeOut(tween(durationMillis = 1000))),
        contentTransformEndToStart = fadeIn(tween(durationMillis = 1000))
            .togetherWith(fadeOut(tween(durationMillis = 1000))),
        content = { index ->
            val series = tvSeries[index]
            // background
            CarouselItemBackground(
                tvSeries = series,
                tmdbImageProvider = tmdbImageProvider,
                modifier = Modifier.fillMaxSize()
            )
            // foreground
            CarouselItemForeground(
                tvSeries = series,
                isCarouselFocused = isCarouselFocused,
                onPlayTapped = { onPlayTapped(series) },
                onDetailsTapped = { onDetailsTapped(series) },
                modifier = Modifier.fillMaxSize()
            )
        }
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BoxScope.CarouselIndicator(
    itemCount: Int,
    activeItemIndex: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(32.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(8.dp)
            }
            .align(Alignment.BottomEnd)
    ) {
        CarouselDefaults.IndicatorRow(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            itemCount = itemCount,
            activeItemIndex = activeItemIndex,
        )
    }
}

@Composable
private fun CarouselItemForeground(
    tvSeries: TvSeries,
    isCarouselFocused: Boolean,
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
                .padding(32.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = tvSeries.name,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(x = 2f, y = 4f),
                        blurRadius = 2f
                    )
                ),
                color = Color.White,
                maxLines = 1
            )
            
            val description = tvSeries.description
            if (!description.isNullOrEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.65f
                        ),
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(x = 2f, y = 4f),
                            blurRadius = 2f
                        )
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            AnimatedVisibility(
                visible = isCarouselFocused,
                content = {
                    WatchNowButton(onPlayTapped = onPlayTapped)
                }
            )
        }
    }
}

@Composable
private fun CarouselItemBackground(
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
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f)
                        )
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
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface,
            focusedContentColor = MaterialTheme.colorScheme.surface,
        ),
        scale = ButtonDefaults.scale(scale = 1f)
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
