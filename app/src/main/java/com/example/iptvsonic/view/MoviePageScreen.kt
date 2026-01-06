package com.example.iptvsonic.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.iptvsonic.model.Movie
import com.example.iptvsonic.model.Category
import com.example.iptvsonic.model.Playlist
import com.example.iptvsonic.repository.PlaylistService
import com.example.iptvsonic.repository.TMDBImageProvider
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import com.example.iptvsonic.presentation.common.MovieCard

@Composable
fun MoviePageScreen(
    playlist: Playlist,
    playlistService: PlaylistService,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onBackPressed: () -> Unit,
    onMovieSelected: (Movie) -> Unit = {},
    onNavigateToSearch: () -> Unit = {}
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
                onTabSelected = onTabSelected,
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
                    contentPadding = PaddingValues(bottom = 108.dp),
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


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CategoryRow(
    category: Category,
    movies: List<Movie>,
    tmdbImageProvider: TMDBImageProvider,
    onMovieSelected: (Movie) -> Unit
) {
    val (lazyRow, firstItem) = remember { FocusRequester.createRefs() }

    Column(
        modifier = Modifier.focusGroup()
    ) {
        Text(
            text = category.name,
            color = Color.White,
            style = TvMaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(
                start = rememberChildPadding().start,
                bottom = 16.dp
            )
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(
                start = rememberChildPadding().start,
                end = rememberChildPadding().end,
            ),
            modifier = Modifier
                .focusRequester(lazyRow)
                .focusRestorer {
                    firstItem
                }
        ) {
            itemsIndexed(movies) { index, movie ->
                val itemModifier = if (index == 0) {
                    Modifier
                        .focusRequester(firstItem)
                        .width(150.dp)
                } else {
                    Modifier.width(150.dp)
                }
                
                MovieCard(
                    movie = movie,
                    tmdbImageProvider = tmdbImageProvider,
                    onClick = { onMovieSelected(movie) },
                    modifier = itemModifier
                )
            }
        }
    }
}