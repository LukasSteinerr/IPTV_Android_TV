package com.example.iptvsonic.mobile_ui

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iptvsonic.model.Movie
import com.example.iptvsonic.model.TvSeries
import com.example.iptvsonic.presentation.common.MovieCard
import com.example.iptvsonic.presentation.common.TvSeriesCard
import com.example.iptvsonic.repository.PlaylistService
import com.example.iptvsonic.repository.TMDBImageProvider
import com.example.iptvsonic.repository.TMDBService
import com.example.iptvsonic.repository.WatchProgressRepository
import com.example.iptvsonic.viewmodel.SearchViewModel
import com.example.iptvsonic.viewmodel.ViewModelFactory

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
                        color = Color.Red
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
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Start typing to search movies and TV shows.",
                        color = Color.White.copy(alpha = 0.5f),
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
        title = { Text("Search", color = Color.White) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
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
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear Search",
                        tint = Color.Gray
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.Gray,
            cursorColor = Color.White,
            focusedLeadingIconColor = Color.White,
            unfocusedLeadingIconColor = Color.Gray,
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
            top = 16.dp,
            bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp,
            start = 16.dp,
            end = 16.dp
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