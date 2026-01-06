package com.example.tv_app.mobile_ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tv_app.model.Movie
import com.example.tv_app.model.TvSeries
import com.example.tv_app.presentation.common.MovieCard
import com.example.tv_app.presentation.common.TvSeriesCard
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.repository.TMDBService
import com.example.tv_app.repository.WatchProgressRepository
import com.example.tv_app.viewmodel.SearchViewModel
import com.example.tv_app.viewmodel.ViewModelFactory

@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    onTvSeriesClick: (TvSeries) -> Unit,
    tmdbImageProvider: TMDBImageProvider = TMDBImageProvider.getInstance() // Inject or instantiate provider
) {
    // Instantiate dependencies and ViewModel Factory for injection
    val tmdbService = remember { TMDBService() }
    val playlistService = remember { PlaylistService() }
    val watchProgressRepository = remember { WatchProgressRepository() }
    val factory = remember {
        ViewModelFactory(
            tmdbService = tmdbService,
            playlistService = playlistService,
            watchProgressRepository = watchProgressRepository
        )
    }

    val viewModel: SearchViewModel = viewModel(factory = factory)

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val combinedResults = remember(searchResults.movies, searchResults.tvShows) {
        // Interleave movies and TV shows for mixed display
        (searchResults.movies + searchResults.tvShows).shuffled()
    }

    // Handle system back button press
    BackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            SearchTopBar(
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = Color.Black // Ensure a dark background for the screen
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            SearchField(
                query = searchQuery,
                onQueryChange = viewModel::updateSearchQuery
            )
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            } else if (combinedResults.isNotEmpty()) {
                SearchResultsGrid(
                    results = combinedResults,
                    tmdbImageProvider = tmdbImageProvider,
                    onMovieClick = onMovieClick,
                    onTvSeriesClick = onTvSeriesClick
                )
            } else if (searchQuery.isNotBlank()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No results found for \"$searchQuery\"",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Start typing to search movies and TV shows.",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                "Search",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.5.sp
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search for titles...", color = Color.White.copy(alpha = 0.5f)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.5f)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear Search",
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color.White.copy(alpha = 0.3f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            cursorColor = Color.White,
            focusedLeadingIconColor = Color.White.copy(alpha = 0.7f),
            unfocusedLeadingIconColor = Color.White.copy(alpha = 0.5f),
            focusedPlaceholderColor = Color.White.copy(alpha = 0.7f),
            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f)
        )
    )
}

@Composable
fun SearchResultsGrid(
    results: List<Any>,
    tmdbImageProvider: TMDBImageProvider,
    onMovieClick: (Movie) -> Unit,
    onTvSeriesClick: (TvSeries) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3), // Responsive grid display
        contentPadding = PaddingValues(
            top = 0.dp,
            bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp,
            start = 24.dp,
            end = 24.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(results) { item ->
            when (item) {
                is Movie -> MovieCard(
                    movie = item,
                    onClick = { onMovieClick(item) },
                    tmdbImageProvider = tmdbImageProvider
                )
                is TvSeries -> TvSeriesCard(
                    tvSeries = item,
                    onClick = { onTvSeriesClick(item) },
                    tmdbImageProvider = tmdbImageProvider
                )
                // Note: The original requirements specified only Movies and TV Shows. Channels excluded for now.
            }
        }
    }
}