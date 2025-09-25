package com.example.tv_app.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
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
    onBackPressed: () -> Unit,
    onChannelSelected: (Channel) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(2) } // Live TV tab
    
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
                onTabSelected = { newTab ->
                    selectedTab = newTab
                    when (newTab) {
                        0 -> onBackPressed() // Navigate back to Movies
                        1 -> { /* Shows - TODO */ }
                        2 -> { /* Live TV - current screen */ }
                        3 -> { /* Favorites - TODO */ }
                        4 -> { /* Search - TODO */ }
                    }
                },
                backgroundColor = Color.Transparent
            )

            // Live TV content
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Live TV coming soon!",
                    color = Color.White.copy(alpha = 0.6f),
                    style = TvMaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}