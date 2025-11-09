package com.example.tv_app.mobile_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.example.tv_app.model.Movie
import com.example.tv_app.model.Category
import com.example.tv_app.model.Playlist
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.repository.TMDBImageProvider
import kotlinx.coroutines.launch
import com.example.tv_app.presentation.common.MovieCard

@Composable
fun MoviePageScreen(
    playlist: Playlist,
    playlistService: PlaylistService,
    onBackPressed: () -> Unit,
    onMovieSelected: (Movie) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var featuredMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var moviesByCategory by remember { mutableStateOf<Map<Long, List<Movie>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val coroutineScope = rememberCoroutineScope()
    val tmdbImageProvider = remember { TMDBImageProvider.getInstance() }

    // Load data when screen is displayed
    LaunchedEffect(playlist.id) {
        coroutineScope.launch {
            try {
                // Get all categories for this playlist (only movie categories)
                categories = playlistService.getCategoriesForPlaylist(playlist.id)
                    .filter { it.isMovie }
                
                // Load movies for each category
                val movieMap = mutableMapOf<Long, List<Movie>>()
                var allMovies = mutableListOf<Movie>()
                
                categories.forEach { category ->
                    val movies = playlistService.getMoviesForCategory(category.id).sortedByDescending { it.added }
                    movieMap[category.id] = movies
                    allMovies.addAll(movies)
                }
                
                moviesByCategory = movieMap
                
                // Get featured movies (movies marked as featured or first few from first category)
                featuredMovies = allMovies.filter { it.isFeatured }.take(5).ifEmpty {
                    allMovies.take(5)
                }
                
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                // Handle error
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(64.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 108.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Featured Section
                if (featuredMovies.isNotEmpty()) {
                    item {
                        FeaturedContent(
                            movies = featuredMovies,
                            onDetailsTapped = onMovieSelected
                        )
                    }
                }

                // Category Rows
                categories.forEach { category ->
                    val movies = moviesByCategory[category.id] ?: emptyList()
                    if (movies.isNotEmpty()) {
                        item {
                            MovieCategoryRow(
                                title = category.name,
                                movies = movies,
                                tmdbImageProvider = tmdbImageProvider,
                                onMovieSelected = onMovieSelected
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun MovieCategoryRow(
    title: String,
    movies: List<Movie>,
    tmdbImageProvider: TMDBImageProvider,
    onMovieSelected: (Movie) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = "See all",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) { 
            items(movies) { movie ->
                MovieCard(
                    movie = movie,
                    tmdbImageProvider = tmdbImageProvider,
                    onClick = { onMovieSelected(movie) },
                    modifier = Modifier.width(120.dp)
                )
            }
        }
    }
}