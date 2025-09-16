package com.example.tv_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tv_app.model.Playlist
import com.example.tv_app.repository.PlaylistService
import kotlinx.coroutines.launch

class PlaylistViewModel(private val playlistService: PlaylistService) : ViewModel() {

    fun addPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistService.addPlaylist(playlist)
        }
    }

    fun clearDatabase() {
        viewModelScope.launch {
            playlistService.clearDatabase()
        }
    }
}