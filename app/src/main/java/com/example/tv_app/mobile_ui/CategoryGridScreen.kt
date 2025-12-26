package com.example.tv_app.mobile_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tv_app.model.Movie
import com.example.tv_app.model.TvSeries
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.presentation.common.MovieCard
import com.example.tv_app.presentation.common.TvSeriesCard
import kotlinx.coroutines.launch

@Composable
fun CategoryGridScreen(
    categoryId: Long,
    categoryName: String,
    playlistService: PlaylistService,
    onNavigateBack: () -> Unit,
    onMovieSelected: (Movie) -> Unit,
    onShowSelected: (TvSeries) -> Unit,
    contentPadding: PaddingValues,
    isMovie: Boolean
) {
    var contentList by remember { mutableStateOf<List<Any>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    val tmdbImageProvider = remember { TMDBImageProvider.getInstance() }

    LaunchedEffect(categoryId, isMovie) {
        coroutineScope.launch {
            isLoading = true
            contentList = try {
                if (isMovie) {
                    playlistService.getMoviesForCategory(categoryId)
                        .sortedByDescending { it.added }
                } else {
                    playlistService.getTvSeriesForCategory(categoryId)
                        .sortedByDescending { it.lastModified }
                }
            } catch (e: Exception) {
                emptyList()
            }
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(
                    top = 80.dp, // 64.dp for TopBar + 16.dp initial offset
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(contentList) { content ->
                    if (isMovie) {
                        val movie = content as Movie
                        MovieCard(
                            movie = movie,
                            tmdbImageProvider = tmdbImageProvider,
                            onClick = { onMovieSelected(movie) }
                        )
                    } else {
                        val series = content as TvSeries
                        TvSeriesCard(
                            tvSeries = series,
                            tmdbImageProvider = tmdbImageProvider,
                            onClick = { onShowSelected(series) }
                        )
                    }
                }
            }
        }

        // Custom App Bar for Category Grid Screen (since Scaffold is in HomeScreen)
        TopBar(title = categoryName, onNavigateBack = onNavigateBack)
    }
}

@Composable
private fun TopBar(title: String, onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp), // Add padding to not touch the end of the screen
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}