package com.example.tv_app.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tv_app.model.Playlist
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.utils.logAnalyticsEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class PlaylistViewModel(private val playlistService: PlaylistService) : ViewModel() {

    // UI State for progress tracking
    var isLoading = mutableStateOf(false)
        private set
    
    var loadingMessage = mutableStateOf("")
        private set
    
    var errorMessage = mutableStateOf("")
        private set
    
    var playlists = mutableStateOf<List<Playlist>>(emptyList())
        private set
    
    private var currentJob: Job? = null

    init {
        loadPlaylists()
    }

    fun addPlaylist(playlist: Playlist) {
        currentJob = viewModelScope.launch {
            try {
                isLoading.value = true
                errorMessage.value = ""
                loadingMessage.value = "Initiating..."

                Log.d("PlaylistViewModel", "Starting to add playlist: ${playlist.name}")
                playlistService.addPlaylist(playlist) { message ->
                    loadingMessage.value = message
                }

                logAnalyticsEvent("playlist_added", mapOf("status" to "success", "type" to playlist.typeName))
                loadingMessage.value = "Playlist added successfully!"
                Log.d("PlaylistViewModel", "Successfully added playlist: ${playlist.name}")
                
                // Reload playlists after successful addition
                loadPlaylists()

            } catch (e: Exception) {
                val fullMessage = e.message.orEmpty()
                
                // Determine specific error code based on message content
                val errorCode = when {
                    fullMessage.contains("Connect timeout has expired") -> "FAILED_TIMEOUT"
                    fullMessage.contains("No address associated with hostname") -> "FAILED_URL_RESOLUTION"
                    fullMessage.contains("401 Unauthorized") -> "FAILED_INVALID_CREDENTIALS" // Assuming this error comes from XtreamService
                    else -> "FAILED_UNKNOWN"
                }

                // 1. Extract error description (part before [url=) and truncate it
                val reasonDesc = fullMessage.substringBefore("[").trim().take(100)
                
                // 2. Extract sanitized URL info (strips credentials)
                val urlWithParams = fullMessage.substringAfter("[url=").substringBefore(",")
                val urlSanitized = urlWithParams
                    .substringBeforeLast("password") // Strip everything from 'password' onward
                    .removeSuffix("&") // Clean up trailing ampersand if password was the last param
                    .removeSuffix("?") // Clean up trailing question mark
                
                logAnalyticsEvent("playlist_added", mapOf(
                    "status" to "failure",
                    "error_code" to errorCode,
                    "reason" to reasonDesc, // Use shortened description
                    "url_info" to urlSanitized.take(100),
                    "type" to playlist.typeName
                ))
                Log.e("PlaylistViewModel", "Error adding playlist", e)
                errorMessage.value = "Failed to add playlist: ${e.message}"
                loadingMessage.value = "Error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clearDatabase() {
        
        currentJob = viewModelScope.launch {
            try {
                isLoading.value = true
                errorMessage.value = ""
                loadingMessage.value = "Clearing database..."
                
                Log.d("PlaylistViewModel", "Starting to clear database")
                playlistService.clearDatabase()
                
                loadingMessage.value = "Database cleared successfully!"
                Log.d("PlaylistViewModel", "Successfully cleared database")
                
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error clearing database", e)
                errorMessage.value = "Failed to clear database: ${e.message}"
                loadingMessage.value = "Error occurred"
            } finally {
                isLoading.value = false
            }
        }
    }
    
    
    fun clearError() {
        errorMessage.value = ""
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            try {
                playlists.value = playlistService.getAllPlaylists()
                Log.d("PlaylistViewModel", "Loaded ${playlists.value.size} playlists")
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error loading playlists", e)
                errorMessage.value = "Failed to load playlists: ${e.message}"
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            try {
                playlistService.deletePlaylist(playlistId)
                loadPlaylists() // Reload the list after deletion
                Log.d("PlaylistViewModel", "Deleted playlist with ID: $playlistId")
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error deleting playlist", e)
                errorMessage.value = "Failed to delete playlist: ${e.message}"
            }
        }
    }
}