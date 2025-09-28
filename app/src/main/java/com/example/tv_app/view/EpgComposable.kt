package com.example.tv_app.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.tv_app.epg.ui.EpgScreen
import com.example.tv_app.model.Channel
import com.example.tv_app.model.Playlist
import com.example.tv_app.repository.PlaylistService

@Composable
fun EpgGuide(
    playlist: Playlist,
    playlistService: PlaylistService,
    onChannelSelected: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    EpgScreen(
        playlist = playlist,
        playlistService = playlistService,
        onChannelSelected = onChannelSelected,
        onBackPressed = { /* Handle back press if needed */ },
        modifier = modifier
    )
}
