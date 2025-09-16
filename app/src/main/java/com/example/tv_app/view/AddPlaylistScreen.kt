package com.example.tv_app.view

import androidx.compose.foundation.layout.*
import androidx.tv.material3.Button
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tv_app.model.Playlist
import com.example.tv_app.model.PlaylistTypeConstants
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.viewmodel.PlaylistViewModel
import com.example.tv_app.viewmodel.PlaylistViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AddPlaylistScreen(
    playlistService: PlaylistService,
    onPlaylistAdded: () -> Unit,
    playlistViewModel: PlaylistViewModel = viewModel(factory = PlaylistViewModelFactory(playlistService))
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var playlistType by remember { mutableStateOf(PlaylistTypeConstants.m3u) }

    // Observe ViewModel state
    val isLoading by playlistViewModel.isLoading
    val loadingMessage by playlistViewModel.loadingMessage
    val errorMessage by playlistViewModel.errorMessage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Playlist Name") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            RadioButton(
                selected = playlistType == PlaylistTypeConstants.m3u,
                onClick = { playlistType = PlaylistTypeConstants.m3u }
            )
            Text("M3U")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = playlistType == PlaylistTypeConstants.xtream,
                onClick = { playlistType = PlaylistTypeConstants.xtream }
            )
            Text("Xtream")
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL") }
        )
        if (playlistType == PlaylistTypeConstants.xtream) {
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress and Status Display
        if (isLoading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = loadingMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        playlistViewModel.cancelOperation()
                    }) {
                        Text("Cancel")
                    }
                }
            }
        }
        
        // Error Display
        if (errorMessage.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        playlistViewModel.clearError()
                    }) {
                        Text("Dismiss")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                if (!isLoading && name.isNotBlank() && url.isNotBlank()) {
                    val newPlaylist = Playlist(
                        name = name,
                        url = url,
                        username = if (playlistType == PlaylistTypeConstants.xtream) username else null,
                        password = if (playlistType == PlaylistTypeConstants.xtream) password else null,
                        typeInt = playlistType
                    )
                    playlistViewModel.addPlaylist(newPlaylist)
                    onPlaylistAdded()
                }
            },
            enabled = !isLoading && name.isNotBlank() && url.isNotBlank()
        ) {
            Text("Add Playlist")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                playlistViewModel.clearDatabase()
            },
            enabled = !isLoading
        ) {
            Text("Clear Database")
        }
    }
}