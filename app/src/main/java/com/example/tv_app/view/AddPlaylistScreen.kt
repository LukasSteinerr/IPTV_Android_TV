package com.example.tv_app.view

import androidx.compose.foundation.layout.*
import androidx.tv.material3.Button
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        Button(onClick = {
            val newPlaylist = Playlist(
                name = name,
                url = url,
                username = if (playlistType == PlaylistTypeConstants.xtream) username else null,
                password = if (playlistType == PlaylistTypeConstants.xtream) password else null,
                typeInt = playlistType
            )
            // In a real app, you would use a ViewModel and CoroutineScope to call this
            // For simplicity, we'll just log it here.
            playlistViewModel.addPlaylist(newPlaylist)
            onPlaylistAdded()
        }) {
            Text("Add Playlist")
        }
    }
}