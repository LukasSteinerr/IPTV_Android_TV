package com.example.tv_app.mobile_ui

import androidx.compose.foundation.layout.Column
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
    onNavigateToSearch: () -> Unit,
    onBackPressed: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column {
        Appbar(
            selectedTab = selectedTab,
            onTabSelected = { newTab ->
                selectedTab = newTab
            },
            onSearchClicked = onNavigateToSearch
        )

        when (selectedTab) {
            0 -> MoviePageScreen(
                playlist = playlist,
                playlistService = playlistService,
                onMovieSelected = onMovieSelected,
                onNavigateToSearch = onNavigateToSearch,
                onBackPressed = onBackPressed
            )
            1 -> ShowsScreen(
                playlist = playlist,
                playlistService = playlistService,
                onShowSelected = onShowSelected,
                onNavigateToSearch = onNavigateToSearch,
                onBackPressed = onBackPressed
            )
            2 -> LiveTVScreen(
                playlist = playlist,
                playlistService = playlistService,
                onChannelSelected = onChannelSelected,
                onBackPressed = onBackPressed
            )
        }
    }
}
