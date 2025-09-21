package com.example.tv_app.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.example.tv_app.model.Playlist
import com.example.tv_app.repository.PlaylistService
import java.text.SimpleDateFormat
import java.util.*
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

@Composable
fun MyPlaylistsScreen(
    playlistService: PlaylistService,
    onNavigateToAddPlaylist: () -> Unit,
    onPlaylistSelected: (Playlist) -> Unit = {}
) {
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    val addButtonFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Load playlists when the screen is first displayed
    LaunchedEffect(Unit) {
        try {
            playlists = playlistService.getAllPlaylists()
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    // Focus management
    LaunchedEffect(playlists) {
        if (playlists.isEmpty()) {
            addButtonFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A1F2E),
                        Color(0xFF0F1419)
                    ),
                    radius = 1200f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Playlists",
                        color = Color.White,
                        style = TvMaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${playlists.size} playlist${if (playlists.size != 1) "s" else ""} available",
                        color = Color.White.copy(alpha = 0.7f),
                        style = TvMaterialTheme.typography.bodyLarge
                    )
                }

                TvIconButton(
                    onClick = onNavigateToAddPlaylist,
                    modifier = Modifier.focusRequester(addButtonFocusRequester),
                    icon = Icons.Default.Add,
                    text = "Add Playlist"
                )
            }

            // Content area
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = TvMaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(64.dp)
                    )
                }
            } else if (playlists.isEmpty()) {
                EmptyPlaylistsState(
                    onAddPlaylist = onNavigateToAddPlaylist
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(playlists) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistSelected(playlist) },
                            onDelete = {
                                playlistToDelete = playlist
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog && playlistToDelete != null) {
            DeletePlaylistDialog(
                playlist = playlistToDelete!!,
                onConfirm = {
                    val playlistId = playlistToDelete!!.id
                    showDeleteDialog = false
                    playlistToDelete = null
                    
                    coroutineScope.launch {
                        try {
                            playlistService.deletePlaylist(playlistId)
                            playlists = playlistService.getAllPlaylists()
                        } catch (e: Exception) {
                            // Handle error - could add error state here
                        }
                    }
                },
                onDismiss = {
                    showDeleteDialog = false
                    playlistToDelete = null
                }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    androidx.tv.material3.Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        colors = androidx.tv.material3.CardDefaults.colors(
            containerColor = Color.Black.copy(alpha = 0.4f),
            contentColor = Color.White,
            focusedContainerColor = TvMaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            focusedContentColor = Color.White
        ),
        border = androidx.tv.material3.CardDefaults.border(
            border = androidx.tv.material3.Border(
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f)
                )
            ),
            focusedBorder = androidx.tv.material3.Border(
                border = BorderStroke(
                    width = 3.dp,
                    color = TvMaterialTheme.colorScheme.primary
                )
            )
        ),
        shape = androidx.tv.material3.CardDefaults.shape(RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                color = Color.White,
                                style = TvMaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = playlist.typeName,
                                color = TvMaterialTheme.colorScheme.primary,
                                style = TvMaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                        
                        androidx.tv.material3.IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete playlist",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "Last updated: ${dateFormatter.format(playlist.lastUpdated)}",
                        color = Color.White.copy(alpha = 0.6f),
                        style = TvMaterialTheme.typography.bodySmall
                    )
                    
                    if (playlist.isXtream) {
                        Text(
                            text = "Username: ${playlist.username ?: "N/A"}",
                            color = Color.White.copy(alpha = 0.6f),
                            style = TvMaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyPlaylistsState(
    onAddPlaylist: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📺",
                style = TvMaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "No Playlists Yet",
                color = Color.White,
                style = TvMaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Add your first playlist to get started watching your favorite channels",
                color = Color.White.copy(alpha = 0.7f),
                style = TvMaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(400.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            TvIconButton(
                onClick = onAddPlaylist,
                icon = Icons.Default.Add,
                text = "Add First Playlist"
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(16.dp)),
        colors = ButtonDefaults.colors(
            containerColor = TvMaterialTheme.colorScheme.primary,
            contentColor = Color.Black,
            focusedContainerColor = TvMaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            focusedContentColor = Color.Black
        ),
        border = ButtonDefaults.border(
            border = Border(BorderStroke(0.dp, Color.Transparent)),
            focusedBorder = Border(BorderStroke(0.dp, Color.Transparent))
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.05f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = TvMaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
fun DeletePlaylistDialog(
    playlist: Playlist,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Playlist",
                style = TvMaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"${playlist.name}\"? This action cannot be undone.",
                style = TvMaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(device = "id:tv_1080p")
@Composable
fun PreviewMyPlaylistsScreenEmpty() {
    TvMaterialTheme {
        MyPlaylistsScreen(
            playlistService = PlaylistService(),
            onNavigateToAddPlaylist = {},
            onPlaylistSelected = {}
        )
    }
}
