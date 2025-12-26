package com.example.tv_app.mobile_ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.tv_app.model.Playlist
import com.example.tv_app.repository.PlaylistService

@Composable
fun HomeScreen(
    playlist: Playlist,
    playlistService: PlaylistService,
    onMovieSelected: (com.example.tv_app.model.Movie) -> Unit,
    onShowSelected: (com.example.tv_app.model.TvSeries) -> Unit,
    onChannelSelected: (com.example.tv_app.model.Channel) -> Unit,
    onBackPressed: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            if (!showSearch) {
                FixedPrimaryAppBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onSearchClick = { showSearch = true }
                )
            }
        }
    ) { contentPadding ->
        if (showSearch) {
            SearchScreen(
                onNavigateBack = { showSearch = false }
            )
        } else {
            when (selectedTab) {
                0 -> MoviePageScreen(
                    playlist = playlist,
                    playlistService = playlistService,
                    onMovieSelected = onMovieSelected,
                    onBackPressed = onBackPressed,
                    contentPadding = contentPadding,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
                1 -> ShowsScreen(
                    playlist = playlist,
                    playlistService = playlistService,
                    onShowSelected = onShowSelected,
                    onBackPressed = onBackPressed,
                    contentPadding = contentPadding,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
                2 -> LiveTVScreen(
                    playlist = playlist,
                    playlistService = playlistService,
                    onChannelSelected = onChannelSelected,
                    onBackPressed = onBackPressed,
                    contentPadding = contentPadding,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }
    }
}
