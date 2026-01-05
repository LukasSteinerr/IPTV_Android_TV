package com.example.tv_app.mobile_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.Support
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector

data class SettingItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit = {}
)

@Composable
fun SettingsItemRow(item: SettingItem) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = item.onClick)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = item.title,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
        Divider(
            color = Color.White.copy(alpha = 0.1f),
            thickness = 1.dp
        )
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val settingItems = listOf(
        SettingItem("Handle Playlists", Icons.Default.List),
        SettingItem("EPG", Icons.Default.DateRange),
        SettingItem("General", Icons.Default.Settings),
        SettingItem("Tell a Friend", Icons.Default.Share),
        SettingItem("Report a Bug", Icons.Default.BugReport),
        SettingItem("Rate", Icons.Default.StarRate),
        SettingItem("Support", Icons.Default.Support),
        SettingItem("Info", Icons.Default.Info)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            // Header inspired by MyPlaylistsScreen
            Text(
                text = "Settings",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp)
            ) {
                items(settingItems) { item ->
                    SettingsItemRow(item)
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewSettingsScreen() {
    MaterialTheme {
        SettingsScreen()
    }
}
