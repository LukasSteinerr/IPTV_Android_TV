package com.example.tv_app.presentation.screens.videoPlayer

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tv_app.model.Movie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoPlayerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<VideoPlayerUiState>(VideoPlayerUiState.Loading)
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    fun loadMovie(movie: Movie) {
        viewModelScope.launch {
            _uiState.value = VideoPlayerUiState.Ready(movie)
        }
    }

    fun loadChannel(streamUrl: String, channelName: String) {
        viewModelScope.launch {
            // Create a movie-like object for channels
            val channelMovie = Movie(
                name = channelName,
                streamUrl = streamUrl,
                description = "Live TV Channel"
            )
            _uiState.value = VideoPlayerUiState.Ready(channelMovie)
        }
    }

    fun reset() {
        viewModelScope.launch {
            _uiState.value = VideoPlayerUiState.Loading
        }
    }
}

@Immutable
sealed class VideoPlayerUiState {
    data object Loading : VideoPlayerUiState()
    data class Ready(val movie: Movie) : VideoPlayerUiState()
    data object Error : VideoPlayerUiState()
}