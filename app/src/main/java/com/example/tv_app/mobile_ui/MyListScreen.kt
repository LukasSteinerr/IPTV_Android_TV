package com.example.tv_app.mobile_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tv_app.model.Movie
import com.example.tv_app.model.ObjectBox
import com.example.tv_app.model.TvSeries
import com.example.tv_app.model.TvSeries_
import com.example.tv_app.model.Movie_
import com.example.tv_app.presentation.common.TMDBPosterImage
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.ui.theme.JetStreamCardShape

// Internal model for unified display
private sealed class MediaItem(
    val id: Long,
    val title: String,
    val tmdbId: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val type: MediaType
) {
    enum class MediaType { MOVIE, SERIES }

    data class MovieItem(val movie: Movie) : MediaItem(
        id = movie.id,
        title = movie.name,
        tmdbId = movie.tmdbId,
        posterUrl = movie.posterUrl ?: movie.coverUrl,
        backdropUrl = movie.backdropUrl,
        type = MediaType.MOVIE
    )

    data class SeriesItem(val series: TvSeries) : MediaItem(
        id = series.id,
        title = series.name,
        tmdbId = series.tmdbId,
        posterUrl = series.coverUrl,
        backdropUrl = null, // TvSeries model lacks backdropUrl in top level usually, generic fallback
        type = MediaType.SERIES
    )
}

@Composable
fun MyListScreen(
    refreshKey: Int,
    onMovieSelected: (Movie) -> Unit = {},
    onTvSeriesSelected: (TvSeries) -> Unit = {},
    modifier: Modifier = Modifier
) {
    
    // State
    val allItems = remember { mutableStateListOf<MediaItem>() }
    var selectedFilter by remember { mutableStateOf(Filter.ALL) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch Data
    LaunchedEffect(refreshKey) {
        // Fetch Movies
        val movieBox = ObjectBox.boxStore.boxFor(Movie::class.java)
        val likedMovies = movieBox.query().equal(Movie_.myList, 1).build().find()
            .map { MediaItem.MovieItem(it) }

        // Fetch TV Series
        val seriesBox = ObjectBox.boxStore.boxFor(TvSeries::class.java)
        val likedSeries = seriesBox.query().equal(TvSeries_.myList, 1).build().find()
            .map { MediaItem.SeriesItem(it) }

        allItems.clear()
        allItems.addAll(likedMovies + likedSeries)
        isLoading = false
    }

    val filteredItems by remember {
        derivedStateOf {
            when (selectedFilter) {
                Filter.ALL -> allItems
                Filter.MOVIES -> allItems.filter { it.type == MediaItem.MediaType.MOVIE }
                Filter.SERIES -> allItems.filter { it.type == MediaItem.MediaType.SERIES }
            }
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            // Header
            Text(
                text = "My List",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == Filter.ALL,
                    onClick = { selectedFilter = Filter.ALL },
                    label = { Text("All", style = MaterialTheme.typography.bodySmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                        containerColor = Color.White.copy(alpha = 0.1f),
                        labelColor = Color.White.copy(alpha = 0.7f)
                    ),
                    border = null
                )
                FilterChip(
                    selected = selectedFilter == Filter.MOVIES,
                    onClick = { selectedFilter = Filter.MOVIES },
                    label = { Text("Movies", style = MaterialTheme.typography.bodySmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                        containerColor = Color.White.copy(alpha = 0.1f),
                        labelColor = Color.White.copy(alpha = 0.7f)
                    ),
                    border = null
                )
                FilterChip(
                    selected = selectedFilter == Filter.SERIES,
                    onClick = { selectedFilter = Filter.SERIES },
                    label = { Text("TV Shows", style = MaterialTheme.typography.bodySmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                        containerColor = Color.White.copy(alpha = 0.1f),
                        labelColor = Color.White.copy(alpha = 0.7f)
                    ),
                    border = null
                )
            }

            // Content Grid
            if (filteredItems.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your list is empty",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Light,
                                letterSpacing = 0.3.sp
                            ),
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(0.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems) { item ->
                        MediaItemCard(
                            item = item,
                            onClick = {
                                when (item) {
                                    is MediaItem.MovieItem -> onMovieSelected(item.movie)
                                    is MediaItem.SeriesItem -> onTvSeriesSelected(item.series)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaItemCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        ) {
            TMDBPosterImage(
                tmdbId = item.tmdbId,
                fallbackUrl = item.posterUrl,
                tmdbImageProvider = TMDBImageProvider.getInstance(),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.1.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White
        )
    }
}

private enum class Filter {
    ALL, MOVIES, SERIES
}
