package com.example.tv_app.mobile_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun FavoritesScreen(
    playlist: Playlist,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    
    Box(
        modifier = modifier
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

            // Favorites content
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Favorites coming soon!",
                    color = Color.White.copy(alpha = 0.6f),
                    style = TvMaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}