package com.example.tv_app.mobile_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.derivedStateOf
import com.example.tv_app.model.Movie
import com.example.tv_app.model.TvSeries
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.repository.TMDBService
import com.example.tv_app.repository.WatchProgressRepository
import com.example.tv_app.viewmodel.ContinueWatchingViewModel
import com.example.tv_app.viewmodel.ContinueWatchingItem
import com.example.tv_app.viewmodel.ViewModelFactory
import com.example.tv_app.presentation.common.MovieCard
import com.example.tv_app.presentation.common.ProgressMovieCard
import com.example.tv_app.presentation.common.TvSeriesCard
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
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
    BackHandler(onBack = onNavigateBack)
    var contentList by remember { mutableStateOf<List<Any>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    val tmdbImageProvider = remember { TMDBImageProvider.getInstance() }
    
    val isContinueWatching = categoryId == 0L

    // Initialize CW ViewModel only if needed (for CW screen)
    val cwViewModel: ContinueWatchingViewModel? = if (isContinueWatching) {
        viewModel(
            factory = remember {
                ViewModelFactory(
                    tmdbService = TMDBService(),
                    playlistService = playlistService,
                    watchProgressRepository = WatchProgressRepository()
                )
            }
        )
    } else null
    
    // Use collectAsStateWithLifecycle to collect the StateFlows safely
    val cwMovieItems by (cwViewModel?.movieProgressItems?.collectAsStateWithLifecycle(emptyList())
        ?: remember { mutableStateOf(emptyList()) })
    
    val cwSeriesItems by (cwViewModel?.seriesProgressItems?.collectAsStateWithLifecycle(emptyList())
        ?: remember { mutableStateOf(emptyList()) })
    
    // Determine the final list to display
    val finalContentList by remember(isContinueWatching, contentList, cwMovieItems, cwSeriesItems) {
        derivedStateOf {
            if (isContinueWatching) {
                if (isMovie) cwMovieItems else cwSeriesItems
            } else {
                contentList.map { it as Any }
            }
        }
    }

    LaunchedEffect(categoryId, isMovie, cwMovieItems, cwSeriesItems) {
        if (!isContinueWatching) {
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
        } else {
             // If CW mode, rely on flow updates.
             isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = categoryName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.9f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                modifier = Modifier.background(Color.Black)
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(finalContentList, key = {
                        if (it is ContinueWatchingItem) it.mediaId else if (it is Movie) it.id else (it as TvSeries).id
                    }) { content ->
                        if (isContinueWatching) {
                            val cwItem = content as ContinueWatchingItem
                            ProgressMovieCard(
                                movie = cwItem.movie,
                                tmdbImageProvider = tmdbImageProvider,
                                progressPercent = (cwItem.watchProgress.positionMillis.toFloat() / cwItem.watchProgress.durationMillis.toFloat()).coerceIn(0f, 1f),
                                onClick = { onMovieSelected(cwItem.movie) }
                            )
                        } else if (isMovie) {
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
        }
    }
}