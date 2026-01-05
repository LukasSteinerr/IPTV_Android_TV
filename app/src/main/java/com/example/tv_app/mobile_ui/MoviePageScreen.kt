package com.example.tv_app.mobile_ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tv_app.model.Category
import com.example.tv_app.model.Playlist
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.repository.TMDBService
import com.example.tv_app.repository.WatchProgressRepository
import com.example.tv_app.viewmodel.ContinueWatchingViewModel
import com.example.tv_app.viewmodel.ContinueWatchingItem
import com.example.tv_app.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import com.example.tv_app.presentation.common.MovieCard
import com.example.tv_app.presentation.common.ProgressMovieCard
import com.example.tv_app.presentation.components.LoadingIndicator
import com.example.tv_app.presentation.components.FullScreenDarkLoading
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoviePageScreen(
    playlist: Playlist,
    playlistService: PlaylistService,
    onBackPressed: () -> Unit,
    onMovieSelected: (Movie) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    onSeeAllClick: (Long, String) -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    lazyListState: LazyListState,
    hazeState: HazeState
) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var featuredMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var moviesByCategory by remember { mutableStateOf<Map<Long, List<Movie>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val coroutineScope = rememberCoroutineScope()
    val tmdbImageProvider = remember { TMDBImageProvider.getInstance() }
    
    // Dependency instantiation for ViewModel
    val tmdbService = remember { TMDBService() }
    val watchProgressRepository = remember { WatchProgressRepository() }

    // Continue Watching ViewModel
    val continueWatchingViewModel: ContinueWatchingViewModel = viewModel(
        factory = remember {
            ViewModelFactory(
                tmdbService = tmdbService,
                playlistService = playlistService,
                watchProgressRepository = watchProgressRepository
            )
        }
    )
    val continueWatchingMovieItems by continueWatchingViewModel.movieProgressItems.collectAsState()

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
            FullScreenDarkLoading(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize().haze(
                    hazeState,
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = Color.Black.copy(alpha = .2f),
                    blurRadius = 30.dp,
                ),
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding() + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp
                ),
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
                
                // Continue Watching Section
                if (continueWatchingMovieItems.isNotEmpty()) {
                    item {
                        ContinueWatchingRow(
                            items = continueWatchingMovieItems,
                            tmdbImageProvider = tmdbImageProvider,
                            onMovieSelected = onMovieSelected,
                            onSeeAllClick = onSeeAllClick
                        )
                    }
                }

                // Category Rows
                categories.forEach { category ->
                    val movies = moviesByCategory[category.id] ?: emptyList()
                    if (movies.isNotEmpty()) {
                        item {
                            MovieCategoryRow(
                                categoryId = category.id,
                                title = category.name,
                                movies = movies,
                                tmdbImageProvider = tmdbImageProvider,
                                onMovieSelected = onMovieSelected,
                                onSeeAllClick = onSeeAllClick
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ContinueWatchingRow(
    items: List<ContinueWatchingItem>,
    tmdbImageProvider: TMDBImageProvider,
    onMovieSelected: (Movie) -> Unit,
    onSeeAllClick: (Long, String) -> Unit // Added missing parameter
) {
    if (items.isEmpty()) return

    Column(modifier = Modifier.padding(top = 10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Continue Watching",
                color = Color(0X8AFFFFFF),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            // See All Button implementation similar to MovieCategoryRow
            Text(
                text = "See all",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(start = 8.dp)
                    // Pass dummy ID (0L) and title for navigation scope
                    .clickable { onSeeAllClick(0L, "Continue Watching") }
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(items, key = { it.mediaId }) { item ->
                val progressPercent = if (item.watchProgress.durationMillis > 0) {
                    item.watchProgress.positionMillis.toFloat() / item.watchProgress.durationMillis.toFloat()
                } else 0f

                ProgressMovieCard(
                    movie = item.movie,
                    tmdbImageProvider = tmdbImageProvider,
                    progressPercent = progressPercent,
                    onClick = { onMovieSelected(item.movie) },
                    modifier = Modifier.width(120.dp)
                )
            }
        }
    }
}


@Composable
fun MovieCategoryRow(
    categoryId: Long,
    title: String,
    movies: List<Movie>,
    tmdbImageProvider: TMDBImageProvider,
    onMovieSelected: (Movie) -> Unit,
    onSeeAllClick: (Long, String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color(0X8AFFFFFF),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = "See all",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { onSeeAllClick(categoryId, title) }
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(movies, key = { it.id }) { movie ->
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
