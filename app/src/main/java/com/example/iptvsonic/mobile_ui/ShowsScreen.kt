package com.example.iptvsonic.mobile_ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
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
import com.example.iptvsonic.model.TvSeries
import com.example.iptvsonic.model.Category
import com.example.iptvsonic.model.Playlist
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iptvsonic.repository.PlaylistService
import com.example.iptvsonic.repository.TMDBImageProvider
import com.example.iptvsonic.repository.TMDBService
import com.example.iptvsonic.repository.WatchProgressRepository
import com.example.iptvsonic.viewmodel.ContinueWatchingViewModel
import com.example.iptvsonic.viewmodel.ContinueWatchingItem
import com.example.iptvsonic.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import com.example.iptvsonic.presentation.common.TvSeriesCard
import com.example.iptvsonic.presentation.common.ProgressMovieCard
import com.example.iptvsonic.model.Movie
import com.example.iptvsonic.presentation.components.FullScreenDarkLoading
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShowsScreen(
    playlist: Playlist,
    playlistService: PlaylistService,
    onBackPressed: () -> Unit,
    onShowSelected: (TvSeries) -> Unit = {},
    onPlayMovie: (Movie) -> Unit, // Added function to launch player
    onNavigateToSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    onSeeAllClick: (Long, String) -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    hazeState: HazeState
) {
   var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var featuredTvSeries by remember { mutableStateOf<List<TvSeries>>(emptyList()) }
    var tvSeriesByCategory by remember { mutableStateOf<Map<Long, List<TvSeries>>>(emptyMap()) }
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
    val continueWatchingSeriesItems by continueWatchingViewModel.seriesProgressItems.collectAsState()

    // Load data when screen is displayed
    LaunchedEffect(playlist.id) {
        coroutineScope.launch {
            try {
                // Get all categories for this playlist (only series categories)
                categories = playlistService.getCategoriesForPlaylist(playlist.id)
                    .filter { it.isSeries }
                
                // Load TV series for each category
                val tvSeriesMap = mutableMapOf<Long, List<TvSeries>>()
                var allTvSeries = mutableListOf<TvSeries>()
                
                categories.forEach { category ->
                    val series = playlistService.getTvSeriesForCategory(category.id).sortedByDescending { it.lastModified }
                    tvSeriesMap[category.id] = series
                    allTvSeries.addAll(series)
                }
                
                tvSeriesByCategory = tvSeriesMap
                
                // Get featured TV series (series marked as featured or first few from first category)
                featuredTvSeries = allTvSeries.filter { it.isFeatured }.take(5).ifEmpty {
                    allTvSeries.take(5)
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
                if (featuredTvSeries.isNotEmpty()) {
                    item {
                        FeaturedTvSeriesContent(
                            tvSeries = featuredTvSeries,
                            onDetailsTapped = onShowSelected
                        )
                    }
                }

                // Continue Watching Series Section
                if (continueWatchingSeriesItems.isNotEmpty()) {
                    item {
                        ContinueWatchingRow(
                            items = continueWatchingSeriesItems,
                            tmdbImageProvider = tmdbImageProvider,
                            onMovieSelected = onPlayMovie, // Use onPlayMovie to launch episode playback
                            onSeeAllClick = onSeeAllClick, // Placeholder navigation for now
                            title = "Continue Watching Series"
                        )
                    }
                }
                
                // Category Rows
                categories.forEach { category ->
                    val series = tvSeriesByCategory[category.id] ?: emptyList()
                    if (series.isNotEmpty()) {
                        item {
                            TvSeriesCategoryRow(
                                categoryId = category.id,
                                title = category.name,
                                tvSeries = series,
                                tmdbImageProvider = tmdbImageProvider,
                                onTvSeriesSelected = onShowSelected,
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
    onSeeAllClick: (Long, String) -> Unit,
    title: String
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
                text = title,
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
                    .clickable { onSeeAllClick(0L, title) }
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
                    movie = item.movie, // Note: This is an episode mapped to Movie model
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
fun TvSeriesCategoryRow(
    categoryId: Long,
    title: String,
    tvSeries: List<TvSeries>,
    tmdbImageProvider: TMDBImageProvider,
    onTvSeriesSelected: (TvSeries) -> Unit,
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
            items(tvSeries) { series ->
                TvSeriesCard(
                    tvSeries = series,
                    tmdbImageProvider = tmdbImageProvider,
                    onClick = { onTvSeriesSelected(series) },
                    modifier = Modifier.width(120.dp)
                )
            }
        }
    }
}