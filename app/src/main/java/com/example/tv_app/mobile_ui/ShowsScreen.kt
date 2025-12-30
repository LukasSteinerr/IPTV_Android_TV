package com.example.tv_app.mobile_ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.tv_app.model.TvSeries
import com.example.tv_app.model.Category
import com.example.tv_app.model.Playlist
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.repository.TMDBImageProvider
import kotlinx.coroutines.launch
import com.example.tv_app.presentation.common.TvSeriesCard
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShowsScreen(
    playlist: Playlist,
    playlistService: PlaylistService,
    onBackPressed: () -> Unit,
    onShowSelected: (TvSeries) -> Unit = {},
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
                state = lazyListState,
                modifier = Modifier.fillMaxSize().haze(
                    hazeState,
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = Color.Black.copy(alpha = .2f),
                    blurRadius = 30.dp,
                ),
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding()
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