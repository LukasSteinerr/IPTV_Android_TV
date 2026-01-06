package com.example.iptvsonic.mobile_ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.iptvsonic.model.Category
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.iptvsonic.model.Playlist
import com.example.iptvsonic.repository.PlaylistService
import dev.chrisbanes.haze.HazeState

@Composable
fun HomeScreen(
    playlist: Playlist,
    playlistService: PlaylistService,
    selectedTab: Int, // Externalized state
    onTabSelected: (Int) -> Unit, // Externalized state updater
    onMovieSelected: (com.example.iptvsonic.model.Movie) -> Unit,
    onShowSelected: (com.example.iptvsonic.model.TvSeries) -> Unit,
    onChannelSelected: (com.example.iptvsonic.model.Channel) -> Unit,
    onBackPressed: () -> Unit,
    hazeState: HazeState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    var showSearch by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryName by remember { mutableStateOf<String?>(null) }

    // Use a key based on selectedTab to force a new LazyListState instance whenever the tab changes,
    // ensuring the scroll position resets to the top.
    val lazyListState = rememberLazyListState()

    // Manually reset scroll state when the selected tab changes
    LaunchedEffect(selectedTab) {
        lazyListState.scrollToItem(0)
    }

    val isScrolled = remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }
    val appBarColor = if ((selectedTab == 0 || selectedTab == 1) && !isScrolled.value) {
        Color.Transparent
    } else {
        Color.Black.copy(alpha = 0.9f)
    }
    
    val onCategorySelected = remember {
        { id: Long, name: String ->
            selectedCategoryId = id
            selectedCategoryName = name
        }
    }
    
    val onCategoryBackClicked = remember {
        {
            selectedCategoryId = null
            selectedCategoryName = null
        }
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            if (!showSearch && selectedCategoryId == null) {
                FixedPrimaryAppBar(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    onSearchClick = { showSearch = true },
                    backgroundColor = appBarColor
                )
            }
        }
    ) { contentPadding ->
        if (showSearch) {
            SearchScreen(
                onNavigateBack = { showSearch = false },
                onMovieClick = onMovieSelected,
                onTvSeriesClick = onShowSelected
            )
        } else if (selectedCategoryId != null && selectedCategoryName != null) {
            // New "See All" screen
            CategoryGridScreen(
                categoryId = selectedCategoryId!!,
                categoryName = selectedCategoryName!!,
                playlistService = playlistService,
                onNavigateBack = onCategoryBackClicked,
                onMovieSelected = onMovieSelected,
                onShowSelected = onShowSelected,
                contentPadding = contentPadding,
                isMovie = selectedTab == 0 // Use selected tab to infer if it's movie or series
            )
        } else {
            when (selectedTab) {
                0 -> MoviePageScreen(
                    playlist = playlist,
                    playlistService = playlistService,
                    onMovieSelected = onMovieSelected,
                    onBackPressed = onBackPressed,
                    contentPadding = contentPadding,
                    onSeeAllClick = onCategorySelected,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    lazyListState = lazyListState,
                    hazeState = hazeState
                )
                1 -> ShowsScreen(
                    playlist = playlist,
                    playlistService = playlistService,
                    onShowSelected = onShowSelected,
                    onPlayMovie = onMovieSelected, // Pass movie player launch function
                    onBackPressed = onBackPressed,
                    contentPadding = contentPadding,
                    onSeeAllClick = onCategorySelected,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    lazyListState = lazyListState,
                    hazeState = hazeState
                )
                2 -> LiveTVScreen(
                    playlist = playlist,
                    playlistService = playlistService,
                    onChannelSelected = onChannelSelected,
                    onBackPressed = onBackPressed,
                    contentPadding = contentPadding,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
            }
        }
    }
}
