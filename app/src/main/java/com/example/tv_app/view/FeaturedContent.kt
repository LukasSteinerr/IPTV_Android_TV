package com.example.tv_app.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.tv_app.model.Movie
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import com.example.tv_app.repository.TMDBImageProvider

@OptIn(ExperimentalPagerApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun FeaturedContent(
    movies: List<Movie>,
    onPlayTapped: (Movie) -> Unit,
    onDetailsTapped: (Movie) -> Unit
) {
    if (movies.isEmpty()) {
        return
    }

    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()
    val tmdbImageProvider = remember { TMDBImageProvider.getInstance() }

    // State to hold the resolved image URL for the current page
    var currentImageUrl by remember { mutableStateOf<String?>(null) }

    // Update the image URL when the current page changes
    LaunchedEffect(pagerState.currentPage) {
        val currentMovie = movies.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        currentImageUrl = tmdbImageProvider.getBackdropUrl(currentMovie.tmdbId)
            ?: tmdbImageProvider.getPosterUrl(currentMovie.tmdbId, currentMovie.posterUrl)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
            .background(Color.Black)
    ) {
        HorizontalPager(
            count = movies.size,
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val movie = movies[page]
            var imageUrl by remember { mutableStateOf<String?>(null) }

            // Fetch the best available image URL for this page
            LaunchedEffect(movie.tmdbId) {
                imageUrl = tmdbImageProvider.getBackdropUrl(movie.tmdbId)
                    ?: tmdbImageProvider.getPosterUrl(movie.tmdbId, movie.posterUrl)
            }

            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 32.dp)
        ) {
            val movie = movies[pagerState.currentPage]
            Text(
                text = movie.name,
                style = androidx.tv.material3.MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Button(
                    onClick = { onPlayTapped(movie) },
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.9f),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_media_play),
                        contentDescription = "Play"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Play")
                }
                Button(
                    onClick = { onDetailsTapped(movie) },
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Details")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in movies.indices) {
                    val color = if (pagerState.currentPage == i) Color.White else Color.Gray
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}