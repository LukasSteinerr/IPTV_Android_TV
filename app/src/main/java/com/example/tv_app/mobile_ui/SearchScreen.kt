package com.example.tv_app.mobile_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import com.example.tv_app.model.Movie
import com.example.tv_app.model.TvSeries
import com.example.tv_app.presentation.common.MovieCard
import com.example.tv_app.presentation.common.TvSeriesCard
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.repository.TMDBService
import com.example.tv_app.viewmodel.SearchViewModel
import com.example.tv_app.viewmodel.SearchViewModelFactory

@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    tmdbImageProvider: TMDBImageProvider = TMDBImageProvider.getInstance() // Inject or instantiate provider
) {
    // Instantiate dependencies and ViewModel Factory for injection
    val tmdbService = remember { TMDBService() }
    val playlistService = remember { PlaylistService() }
    val factory = remember { SearchViewModelFactory(tmdbService, playlistService) }

    val viewModel: SearchViewModel = viewModel(factory = factory)

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val combinedResults = remember(searchResults.movies, searchResults.tvShows) {
        // Interleave movies and TV shows for mixed display
        (searchResults.movies + searchResults.tvShows).shuffled()
    }

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
                    tmdbImageProvider = tmdbImageProvider
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
    tmdbImageProvider: TMDBImageProvider
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3), // Responsive grid display
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(results) { item ->
            when (item) {
                is Movie -> MovieCard(
                    movie = item,
                    onClick = { /* Handle movie click */ },
                    tmdbImageProvider = tmdbImageProvider
                )
                is TvSeries -> TvSeriesCard(
                    tvSeries = item,
                    onClick = { /* Handle TV series click */ },
                    tmdbImageProvider = tmdbImageProvider
                )
                // Note: The original requirements specified only Movies and TV Shows. Channels excluded for now.
            }
        }
    }
}