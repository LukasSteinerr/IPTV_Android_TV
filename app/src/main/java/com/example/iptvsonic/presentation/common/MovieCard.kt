package com.example.iptvsonic.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import coil.compose.SubcomposeAsyncImage
import com.example.iptvsonic.model.Movie
import com.example.iptvsonic.repository.TMDBImageProvider
import com.example.iptvsonic.ui.theme.JetStreamCardShape
import androidx.compose.ui.graphics.Color

// New Composable to handle image loading logic, closely mirroring Flutter's TMDBImage
@Composable
fun TMDBPosterImage(
    tmdbId: String?,
    fallbackUrl: String?,
    tmdbImageProvider: TMDBImageProvider,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var posterUrl by remember(tmdbId, fallbackUrl) { mutableStateOf<String?>(null) }
    
    // Asynchronously fetch the high-res URL if a TMDB ID exists
    LaunchedEffect(tmdbId, fallbackUrl) {
        // 1. Attempt to get poster URL (which handles local fallback)
        var resultUrl = tmdbImageProvider.getPosterUrl(tmdbId, fallbackUrl)

        // 2. If result is null (meaning TMDB poster failed AND local poster/backdrop URL in Movie model was null),
        //    try fetching the backdrop URL directly from TMDB as a final visual fallback.
        if (resultUrl.isNullOrEmpty() && !tmdbId.isNullOrEmpty()) {
            resultUrl = tmdbImageProvider.getBackdropUrl(tmdbId)
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
fun MovieCard(
    movie: Movie,
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
            TMDBPosterImage(
                tmdbId = movie.tmdbId,
                // Use posterUrl first, then backdropUrl if posterUrl is null
                fallbackUrl = movie.posterUrl ?: movie.backdropUrl ?: movie.coverUrl,
                tmdbImageProvider = tmdbImageProvider,
                contentDescription = movie.name,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        if (showTitle) {
            Text(
                text = movie.name,
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

@Composable
fun ProgressMovieCard(
    movie: Movie,
    tmdbImageProvider: TMDBImageProvider,
    progressPercent: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
                .aspectRatio(10.5f / 16f)
        ) {
            Box(Modifier.fillMaxSize()) {
                TMDBPosterImage(
                    tmdbId = movie.tmdbId,
                    fallbackUrl = movie.posterUrl ?: movie.backdropUrl ?: movie.coverUrl,
                    tmdbImageProvider = tmdbImageProvider,
                    contentDescription = movie.name,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Progress Bar at the bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressPercent.coerceIn(0f, 1f))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        Text(
            text = movie.name,
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
