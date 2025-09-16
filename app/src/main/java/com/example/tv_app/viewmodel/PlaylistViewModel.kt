package com.example.tv_app.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tv_app.model.Playlist
import com.example.tv_app.repository.PlaylistService
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException

class PlaylistViewModel(private val playlistService: PlaylistService) : ViewModel() {

    // UI State for progress tracking
    var isLoading = mutableStateOf(false)
        private set
    
    var loadingMessage = mutableStateOf("")
        private set
    
    var errorMessage = mutableStateOf("")
        private set
    
    private var currentJob: Job? = null

    fun addPlaylist(playlist: Playlist) {
        // Cancel any existing job
        currentJob?.cancel()
        
        currentJob = viewModelScope.launch {
            try {
                isLoading.value = true
                errorMessage.value = ""
                loadingMessage.value = "Adding playlist..."
                
                Log.d("PlaylistViewModel", "Starting to add playlist: ${playlist.name}")
                playlistService.addPlaylist(playlist)
                
                loadingMessage.value = "Playlist added successfully!"
                Log.d("PlaylistViewModel", "Successfully added playlist: ${playlist.name}")
                
            } catch (e: CancellationException) {
                Log.d("PlaylistViewModel", "Playlist addition cancelled")
                loadingMessage.value = "Operation cancelled"
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error adding playlist", e)
                errorMessage.value = "Failed to add playlist: ${e.message}"
                loadingMessage.value = "Error occurred"
            } finally {
                isLoading.value = false
                // Clear messages after a delay in real implementation
            }
        }
    }

    fun clearDatabase() {
        currentJob?.cancel()
        
        currentJob = viewModelScope.launch {
            try {
                isLoading.value = true
                errorMessage.value = ""
                loadingMessage.value = "Clearing database..."
                
                Log.d("PlaylistViewModel", "Starting to clear database")
                playlistService.clearDatabase()
                
                loadingMessage.value = "Database cleared successfully!"
                Log.d("PlaylistViewModel", "Successfully cleared database")
                
            } catch (e: CancellationException) {
                Log.d("PlaylistViewModel", "Database clear cancelled")
                loadingMessage.value = "Operation cancelled"
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error clearing database", e)
                errorMessage.value = "Failed to clear database: ${e.message}"
                loadingMessage.value = "Error occurred"
            } finally {
                isLoading.value = false
            }
        }
    }
    
    fun cancelOperation() {
        currentJob?.cancel()
        isLoading.value = false
        loadingMessage.value = "Operation cancelled"
        Log.d("PlaylistViewModel", "Operation cancelled by user")
    }
    
    fun clearError() {
        errorMessage.value = ""
    }
}