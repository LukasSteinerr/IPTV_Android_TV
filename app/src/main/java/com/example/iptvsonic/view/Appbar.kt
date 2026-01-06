package com.example.iptvsonic.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Appbar(
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onSearchClicked: () -> Unit = {},
    backgroundColor: Color = Color.Black.copy(alpha = 0.8f)
) {
    val tabs = listOf("Movies", "Shows", "Live TV", "Favorites")
    val focusRequesters = remember { List(tabs.size) { FocusRequester() } }
    val searchFocusRequester = remember { FocusRequester() }
    
    // Request focus on the selected tab when it changes
    LaunchedEffect(selectedTab) {
        if (selectedTab in tabs.indices) {
            focusRequesters[selectedTab].requestFocus()
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            )
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title
        Text(
            text = "IPTV App",
            color = Color.White,
            style = TvMaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
        
        // Navigation Tabs
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tabName ->
                AppbarTab(
                    text = tabName,
                    isSelected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    focusRequester = focusRequesters[index]
                )
            }
            
            // Search Icon
            androidx.tv.material3.IconButton(
                onClick = onSearchClicked,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .focusRequester(searchFocusRequester),
                colors = androidx.tv.material3.IconButtonDefaults.colors(
                    containerColor = if (selectedTab == 4) TvMaterialTheme.colorScheme.primary
                    else Color.Transparent,
                    contentColor = Color.White,
                    focusedContainerColor = TvMaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    focusedContentColor = TvMaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppbarTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester
) {
    androidx.tv.material3.Button(
        onClick = onClick,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .focusRequester(focusRequester),
        colors = androidx.tv.material3.ButtonDefaults.colors(
            containerColor = if (isSelected) TvMaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else Color.Transparent,
            contentColor = if (isSelected) TvMaterialTheme.colorScheme.primary
            else Color.White.copy(alpha = 0.8f),
            focusedContainerColor = TvMaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            focusedContentColor = TvMaterialTheme.colorScheme.onPrimary
        ),
        shape = androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(8.dp)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = TvMaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}