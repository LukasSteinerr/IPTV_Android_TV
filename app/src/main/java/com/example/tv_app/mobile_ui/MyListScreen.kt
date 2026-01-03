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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.displayCutout
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
    onMovieSelected: (Movie) -> Unit = {},
    onTvSeriesSelected: (TvSeries) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // State
    val allItems = remember { mutableStateListOf<MediaItem>() }
    var selectedFilter by remember { mutableStateOf(Filter.ALL) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch Data
    LaunchedEffect(Unit) {
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
        // Sort by most recently added? ObjectBox doesn't track "liked time" by default easily without a new field.
        // For now, just shuffle or keep retrieval order.
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black) // Base background
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.displayCutout)
    ) {
        // 1. Header & Filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A1A1A), Color.Black)
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                text = "My List",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == Filter.ALL,
                    onClick = { selectedFilter = Filter.ALL },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF333333),
                        labelColor = Color.White
                    ),
                    border = null
                )
                FilterChip(
                    selected = selectedFilter == Filter.MOVIES,
                    onClick = { selectedFilter = Filter.MOVIES },
                    label = { Text("Movies") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF333333),
                        labelColor = Color.White
                    ),
                    border = null
                )
                FilterChip(
                    selected = selectedFilter == Filter.SERIES,
                    onClick = { selectedFilter = Filter.SERIES },
                    label = { Text("TV Shows") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF333333),
                        labelColor = Color.White
                    ),
                    border = null
                )
            }
        }

        // 2. Content Grid
        if (filteredItems.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier
                            .width(64.dp)
                            .height(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your list is empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "Add movies and shows to track what you want to watch.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
            shape = JetStreamCardShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f) // Standard poster aspect ratio
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
                fontWeight = FontWeight.SemiBold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White
        )
    }
}

private enum class Filter {
    ALL, MOVIES, SERIES
}
