package com.example.tv_app.mobile_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import com.example.tv_app.model.Playlist

@Composable
fun SearchScreen(
    playlist: Playlist,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    
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
            // Search content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Search input
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    placeholder = { 
                        Text(
                            text = "Search movies, shows, channels...",
                            color = Color.White.copy(alpha = 0.6f)
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = TvMaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = TvMaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Search results or placeholder
                if (searchQuery.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "Start typing to search",
                                color = Color.White.copy(alpha = 0.6f),
                                style = TvMaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                } else {
                    // TODO: Implement actual search functionality
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Search results for: $searchQuery",
                            color = Color.White.copy(alpha = 0.6f),
                            style = TvMaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}