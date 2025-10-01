package com.example.tv_app.view

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
import com.example.tv_app.model.TvSeries
import com.example.tv_app.model.Category
import com.example.tv_app.model.Playlist
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.repository.TMDBImageProvider
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import com.example.tv_app.presentation.common.TvSeriesCard

@Composable
fun ShowsScreen(
    playlist: Playlist,
    playlistService: PlaylistService,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onBackPressed: () -> Unit,
    onShowSelected: (TvSeries) -> Unit = {},
    onNavigateToSearch: () -> Unit = {}
) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var featuredTvSeries by remember { mutableStateOf<List<TvSeries>>(emptyList()) }
    var tvSeriesByCategory by remember { mutableStateOf<Map<Long, List<TvSeries>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val coroutineScope = rememberCoroutineScope()
    val tmdbImageProvider = remember { TMDBImageProvider.getInstance() }

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
                    val series = playlistService.getTvSeriesForCategory(category.id)
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
                    if (featuredTvSeries.isNotEmpty()) {
                        item {
                            FeaturedTvSeriesContent(
                                tvSeries = featuredTvSeries,
                                onPlayTapped = onShowSelected,
                                onDetailsTapped = onShowSelected
                            )
                        }
                    }

                    // Category Rows
                    categories.forEach { category ->
                        val series = tvSeriesByCategory[category.id] ?: emptyList()
                        if (series.isNotEmpty()) {
                            item {
                                TvSeriesCategoryRow(
                                    category = category,
                                    tvSeries = series,
                                    tmdbImageProvider = tmdbImageProvider,
                                    onTvSeriesSelected = onShowSelected
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}