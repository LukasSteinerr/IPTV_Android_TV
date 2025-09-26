package com.example.tv_app.view

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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.LocalContentColor
import com.example.tv_app.ui.components.AppBarTabIndicator
import com.example.tv_app.ui.theme.JetStreamCardShape
import com.example.tv_app.ui.theme.JetStreamButtonShape

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

    LaunchedEffect(selectedTab) {
        if (selectedTab >= 0 && selectedTab < focusRequesters.size) {
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
        
        // Navigation Tabs with Jetstream-style TabRow
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var isTabRowFocused by remember { mutableStateOf(false) }
            
            TabRow(
                modifier = Modifier
                    .onFocusChanged {
                        isTabRowFocused = it.isFocused || it.hasFocus
                    },
                selectedTabIndex = selectedTab,
                indicator = { tabPositions, _ ->
                    if (selectedTab >= 0 && selectedTab < tabPositions.size) {
                        AppBarTabIndicator(
                            currentTabPosition = tabPositions[selectedTab],
                            anyTabFocused = isTabRowFocused,
                            shape = JetStreamCardShape
                        )
                    }
                },
                separator = { Spacer(modifier = Modifier) }
            ) {
                tabs.forEachIndexed { index, tabName ->
                    Tab(
                        modifier = Modifier
                            .height(32.dp)
                            .focusRequester(focusRequesters[index])
                            .background(
                                color = when {
                                    isTabRowFocused && selectedTab == index -> TvMaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    selectedTab == index -> Color.White.copy(alpha = 0.1f)
                                    else -> Color.Transparent
                                },
                                shape = JetStreamButtonShape
                            ),
                        selected = index == selectedTab,
                        onFocus = { onTabSelected(index) },
                        onClick = { onTabSelected(index) },
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 16.dp),
                            text = tabName,
                            style = TvMaterialTheme.typography.bodyLarge.copy(
                                color = if (isTabRowFocused && selectedTab == index) TvMaterialTheme.colorScheme.onPrimary else Color.White,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(24.dp))
            
            // Search Icon
            androidx.tv.material3.IconButton(
                onClick = onSearchClicked,
                modifier = Modifier
                    .size(40.dp),
                shape = androidx.tv.material3.IconButtonDefaults.shape(JetStreamButtonShape),
                colors = androidx.tv.material3.IconButtonDefaults.colors(
                    containerColor = if (selectedTab == 4) TvMaterialTheme.colorScheme.primary
                    else Color.White.copy(alpha = 0.1f),
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

// This function is now replaced by the TabRow implementation above
// Keeping it commented for reference, but it's no longer used
/*
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppbarTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    androidx.tv.material3.Button(
        onClick = onClick,
        modifier = Modifier,
        colors = androidx.tv.material3.ButtonDefaults.colors(
            containerColor = if (isSelected) TvMaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else Color.Transparent,
            contentColor = if (isSelected) TvMaterialTheme.colorScheme.primary
            else Color.White.copy(alpha = 0.8f),
            focusedContainerColor = TvMaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            focusedContentColor = TvMaterialTheme.colorScheme.onPrimary
        ),
        shape = androidx.tv.material3.ButtonDefaults.shape(JetStreamButtonShape), // Using Jetstream shape
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
*/