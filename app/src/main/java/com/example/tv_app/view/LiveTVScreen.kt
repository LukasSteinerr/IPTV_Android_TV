package com.example.tv_app.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Text
import com.example.tv_app.model.Playlist
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.model.Channel

@Composable
fun LiveTVScreen(
    playlist: Playlist,
    playlistService: PlaylistService,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onBackPressed: () -> Unit,
    onChannelSelected: (Channel) -> Unit = {}
) {
    
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
                backgroundColor = Color.Transparent
            )

            // Live TV content - EPG Guide
            EpgGuide(
                playlist = playlist,
                playlistService = playlistService,
                onChannelSelected = onChannelSelected,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}