package com.example.tv_app.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import coil.compose.AsyncImage
import com.example.tv_app.model.Movie
import com.example.tv_app.model.Category
import com.example.tv_app.model.Playlist
import com.example.tv_app.model.ContentType
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.repository.TMDBImageProvider
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke

@Composable
fun MoviePageScreen(
    playlist: Playlist,
    playlistService: PlaylistService,
    onBackPressed: () -> Unit,
    onMovieSelected: (Movie) -> Unit = {},
    onNavigateToShows: () -> Unit = {},
    onNavigateToLiveTV: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {}
) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var featuredMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var moviesByCategory by remember { mutableStateOf<Map<Long, List<Movie>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    
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
                    val movies = playlistService.getMoviesForCategory(category.id)
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
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A1F2E),
                        Color(0xFF0F1419)
                    ),
                    radius = 1200f
                )
            )
    ) {
        Column {
            // Appbar with matching background
            Appbar(
                selectedTab = selectedTab,
                onTabSelected = { newTab ->
                    selectedTab = newTab
                    // Handle tab navigation here
                    when (newTab) {
                        0 -> { /* Movies - current screen */ }
                        1 -> onNavigateToShows()
                        2 -> onNavigateToLiveTV()
                        3 -> onNavigateToFavorites()
                    }
                },
                onSearchClicked = onNavigateToSearch,
                backgroundColor = Color.Transparent // Make appbar blend with background
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = TvMaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(64.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    // Featured Section
                    if (featuredMovies.isNotEmpty()) {
                        item {
                            FeaturedContent(
                                movies = featuredMovies,
                                onPlayTapped = onMovieSelected,
                                onDetailsTapped = onMovieSelected
                            )
                        }
                    }

                    // Category Rows
                    categories.forEach { category ->
                        val movies = moviesByCategory[category.id] ?: emptyList()
                        if (movies.isNotEmpty()) {
                            item {
                                CategoryRow(
                                    category = category,
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
}


@Composable
fun CategoryRow(
    category: Category,
    movies: List<Movie>,
    tmdbImageProvider: TMDBImageProvider,
    onMovieSelected: (Movie) -> Unit
) {
    Column {
        Text(
            text = category.name,
            color = Color.White,
            style = TvMaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 48.dp)
        ) {
            items(movies) { movie ->
                MovieCard(
                    movie = movie,
                    tmdbImageProvider = tmdbImageProvider,
                    onClick = { onMovieSelected(movie) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieCard(
    movie: Movie,
    tmdbImageProvider: TMDBImageProvider,
    onClick: () -> Unit
) {
    var posterUrl by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(movie.tmdbId) {
        posterUrl = tmdbImageProvider.getPosterUrl(movie.tmdbId, movie.posterUrl)
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .height(270.dp),
        colors = CardDefaults.colors(
            containerColor = Color.Black.copy(alpha = 0.4f),
            contentColor = Color.White,
            focusedContainerColor = TvMaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            focusedContentColor = Color.White
        ),
        border = CardDefaults.border(
            border = androidx.tv.material3.Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ),
            focusedBorder = androidx.tv.material3.Border(
                border = BorderStroke(2.dp, TvMaterialTheme.colorScheme.primary)
            )
        ),
        shape = CardDefaults.shape(RoundedCornerShape(12.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Movie Poster
            AsyncImage(
                model = posterUrl ?: movie.posterUrl,
                contentDescription = movie.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            
            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 180f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            
            // Movie Title
            Text(
                text = movie.name,
                color = Color.White,
                style = TvMaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Rating badge
            if (!movie.rating.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "★ ${movie.rating}",
                        color = TvMaterialTheme.colorScheme.primary,
                        style = TvMaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
