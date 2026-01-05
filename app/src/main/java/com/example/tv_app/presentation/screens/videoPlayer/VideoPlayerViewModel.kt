package com.example.tv_app.presentation.screens.videoPlayer

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tv_app.model.Movie
import com.example.tv_app.model.TvEpisode
import com.example.tv_app.repository.WatchProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoPlayerViewModel : ViewModel() {
    private val watchProgressRepository = WatchProgressRepository()

    private val _uiState = MutableStateFlow<VideoPlayerUiState>(VideoPlayerUiState.Loading)
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()
    
    // State for tracking playback progress
    private var mediaId: String? = null
    private var currentMediaType: String? = null // "movie", "episode", or "channel"
    private var currentPositionMillis: Long = 0L
    private var currentDurationMillis: Long = 0L

    fun updateCurrentPosition(position: Long, duration: Long) {
        currentPositionMillis = position
        currentDurationMillis = duration
    }

    override fun onCleared() {
        super.onCleared()
        // Ensure that if the system clears the ViewModel, we save the progress.
        // Calling saveCurrentProgress() here relies on the latest values set by updateCurrentPosition().
        saveCurrentProgress()
    }
    
    // Public method to manually trigger saving from the UI (e.g., on BackPress)
    fun saveCurrentProgress() {
        Log.d("VideoPlayerVM", "Save attempt triggered. mediaId: $mediaId, pos: $currentPositionMillis, dur: $currentDurationMillis")
        
        mediaId?.let { id ->
            val type = currentMediaType ?: "movie" // Default to movie if null, though should be set in load*
            if (currentDurationMillis > 0) {
                watchProgressRepository.saveProgress(id, type, currentPositionMillis, currentDurationMillis)
            } else {
                Log.w("VideoPlayerVM", "Save skipped: Duration is 0 or less.")
            }
        }
    }

    fun loadMovie(movie: Movie) {
        viewModelScope.launch {
            mediaId = movie.streamId ?: if (movie.id > 0) "movie-${movie.id}" else null
            currentMediaType = "movie"
            
            // Resume playback if progress exists
            val startPosition = mediaId?.let { watchProgressRepository.getSavedPosition(it) } ?: 0L
            
            _uiState.value = VideoPlayerUiState.Ready(movie, startPosition)
        }
    }

    fun loadEpisode(episode: TvEpisode) {
        viewModelScope.launch {
            mediaId = "episode-${episode.id}" // Assuming TvEpisode has an id field or can derive a unique ID
            currentMediaType = "episode"
            
            // Convert episode to a movie-like object for the player
            val episodeMovie = Movie(
                id = episode.id, // Ensure ID is passed for potential fallback mediaId generation
                streamId = mediaId,
                name = episode.name.ifEmpty { episode.title },
                streamUrl = episode.streamUrl,
                description = episode.description ?: "Episode ${episode.episodeNumber}",
                duration = episode.duration
            )

            // Resume playback if progress exists
            val startPosition = mediaId?.let { watchProgressRepository.getSavedPosition(it) } ?: 0L
            
            _uiState.value = VideoPlayerUiState.Ready(episodeMovie, startPosition)
        }
    }

    fun loadChannel(streamUrl: String, channelName: String) {
        viewModelScope.launch {
            // Live TV channels typically don't save progress, but we need a unique ID for the media
            mediaId = "channel-${streamUrl.hashCode()}"
            currentMediaType = "channel"
            
            // Create a movie-like object for channels
            val channelMovie = Movie(
                name = channelName,
                streamUrl = streamUrl,
                description = "Live TV Channel"
            )
            _uiState.value = VideoPlayerUiState.Ready(channelMovie, 0L) // Always start channels from 0
        }
    }

    fun reset() {
        // When reset, clear the mediaId to prevent accidental saving of position
        mediaId = null
        currentMediaType = null
        currentPositionMillis = 0L
        currentDurationMillis = 0L
        viewModelScope.launch {
            _uiState.value = VideoPlayerUiState.Loading
        }
    }
}

@Immutable
sealed class VideoPlayerUiState {
    data object Loading : VideoPlayerUiState()
    data class Ready(val movie: Movie, val startPositionMillis: Long) : VideoPlayerUiState()
    data object Error : VideoPlayerUiState()
}