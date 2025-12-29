package com.example.tv_app.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import coil.compose.SubcomposeAsyncImage
import com.example.tv_app.model.TvSeries
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.ui.theme.JetStreamCardShape
import androidx.compose.ui.graphics.Color

// Composable function to handle image loading for TV Series, mirroring MovieCard logic
@Composable
fun TMDBTvPosterImage(
    tmdbId: String?,
    fallbackUrl: String?, // This will be tvSeries.coverUrl
    tmdbImageProvider: TMDBImageProvider,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var posterUrl by remember(tmdbId, fallbackUrl) { mutableStateOf<String?>(null) }
    
    // Asynchronously fetch the high-res URL if a TMDB ID exists
    LaunchedEffect(tmdbId, fallbackUrl) {
        // 1. Attempt to get poster URL (which handles local fallback)
        var resultUrl = tmdbImageProvider.getTvPosterUrl(tmdbId, fallbackUrl)

        // 2. If result is null, try fetching the backdrop URL directly from TMDB as a final visual fallback.
        if (resultUrl.isNullOrEmpty() && !tmdbId.isNullOrEmpty()) {
            resultUrl = tmdbImageProvider.getTvBackdropUrl(tmdbId)
        }
        
        posterUrl = resultUrl
    }

    // Use SubcomposeAsyncImage to handle asynchronous loading states
    val imageModel = posterUrl ?: fallbackUrl
    SubcomposeAsyncImage(
        model = imageModel,
        contentDescription = contentDescription,
        modifier = modifier.clip(JetStreamCardShape),
        contentScale = ContentScale.Crop,
        loading = {
            // Show the skeletal loading screen while the image is loading (Coil's loading or initial URL fetch)
            NetflixStyleLoading(
                modifier = Modifier.fillMaxSize(),
                borderRadius = 8.dp
            )
        },
        error = {
            // Show the fallback placeholder on image loading error or invalid URL
            ImageFallbackPlaceholder(
                modifier = Modifier.fillMaxSize(),
                borderRadius = 8.dp
            )
        }
    )
}


@Composable
fun TvSeriesCard(
    tvSeries: TvSeries,
    tmdbImageProvider: TMDBImageProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Card(
            shape = JetStreamCardShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(10.5f / 16f) // Maintain aspect ratio of a movie poster (approx. 0.656)
        ) {
            TMDBTvPosterImage(
                tmdbId = tvSeries.tmdbId,
                fallbackUrl = tvSeries.coverUrl, // Use coverUrl as the single local fallback
                tmdbImageProvider = tmdbImageProvider,
                contentDescription = tvSeries.name,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showTitle) {
            Text(
                text = tvSeries.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
        }
    }
}
