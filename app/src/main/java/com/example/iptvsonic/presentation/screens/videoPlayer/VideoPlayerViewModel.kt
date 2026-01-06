package com.example.iptvsonic.presentation.screens.videoPlayer

import android.util.Log
import androidx.compose.runtime.Immutable
import com.example.iptvsonic.utils.logAnalyticsEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvsonic.model.Movie
import com.example.iptvsonic.model.TvEpisode
import com.example.iptvsonic.model.Channel
import com.example.iptvsonic.model.DownloadedMovie
import com.example.iptvsonic.repository.WatchProgressRepository
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
        saveCurrentProgress(logStopEvent = false)
    }
    
    // Public method to manually trigger saving from the UI (e.g., on BackPress)
    fun saveCurrentProgress(logStopEvent: Boolean = true) {
        if (logStopEvent) {
            logPlaybackStop(currentPositionMillis, currentDurationMillis)
        }
        
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
            
            logPlaybackStart(movie.streamId, currentMediaType, startPosition)
            _uiState.value = VideoPlayerUiState.Ready(movie, startPosition, isLive = false)
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
            
            logPlaybackStart(episodeMovie.streamId, currentMediaType, startPosition)
            _uiState.value = VideoPlayerUiState.Ready(episodeMovie, startPosition, isLive = false)
        }
    }

    fun loadDownloadedMedia(download: com.example.iptvsonic.model.DownloadedMovie) {
        viewModelScope.launch {
            // Note: Since this is a local file, we use the unique download ID (movieId in DownloadedMovie) to load/save position.

            val uniqueId = download.movieId
            mediaId = uniqueId
            currentMediaType = if (download.mediaType == com.example.iptvsonic.model.DownloadedMovie.TYPE_TVEPISODE) "episode" else "movie"

            // Construct a Movie object pointing to the local file URI
            val localMovie = Movie(
                id = download.id,
                streamId = uniqueId,
                name = download.movieName,
                streamUrl = "file://${download.localPath}", // Crucial change: use file URI
                description = download.seriesName ?: download.movieName,
                posterUrl = download.posterUrl,
                duration = null
            )

            // Resume playback if progress exists (using the DownloadedMovie ID)
            val startPosition = uniqueId.let { watchProgressRepository.getSavedPosition(it) } ?: 0L

            logPlaybackStart(localMovie.streamId, currentMediaType, startPosition)
            Log.d("VideoPlayerVM", "Loading downloaded media. Path: ${localMovie.streamUrl}, StartPos: $startPosition")
            _uiState.value = VideoPlayerUiState.Ready(localMovie, startPosition, isLive = false)
        }
    }

    fun loadChannel(channel: Channel) {
        viewModelScope.launch {
            // Live TV channels typically don't save progress, but we need a unique ID for the media
            // Using a combination of playlist ID and channel stream URL hash to ensure uniqueness
            val playlistId = channel.playlist.targetId
            mediaId = "channel-${playlistId}-${channel.streamUrl.hashCode()}"
            currentMediaType = "channel"

            // Create a movie-like object for channels to pass to the player
            val channelMovie = Movie(
                id = channel.id,
                streamId = mediaId,
                name = channel.name,
                streamUrl = channel.streamUrl,
                description = "Live TV Channel ${channel.name}",
                posterUrl = channel.logoUrl // Use logo as poster
            )
            logPlaybackStart(channelMovie.streamId, currentMediaType, 0L)
            _uiState.value = VideoPlayerUiState.Ready(channelMovie, 0L, isLive = true) // Always start channels from 0
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
    
    // --- Analytics Logging Functions ---

    private fun logPlaybackStart(mediaId: String?, mediaType: String?, startPosition: Long) {
        if (mediaId == null || mediaType == null) return

        logAnalyticsEvent(
            eventName = "playback_start",
            params = mapOf(
                "content_id" to mediaId,
                "content_type" to mediaType,
                "start_position_ms" to startPosition.toString()
            )
        )
    }

    // Placeholder to be called by the VideoPlayer composable/Exoplayer listener
    fun logPlaybackFailure(errorCode: String, errorMessage: String?) {
        val type = currentMediaType ?: "unknown"
        val id = mediaId ?: "unknown"

        logAnalyticsEvent(
            eventName = "playback_failure",
            params = mapOf(
                "content_id" to id,
                "content_type" to type,
                "error_code" to errorCode.take(100),
                "error_message" to errorMessage.orEmpty().take(100)
            )
        )
        // Optionally, log non-fatal crash for better detail
        // logNonFatalCrash(RuntimeException("Playback Error: $errorCode - $errorMessage"))
    }

    fun logPlaybackStop(position: Long, duration: Long) {
        val id = mediaId
        val type = currentMediaType

        if (id == null || type == null) {
            Log.d("VideoPlayerVM", "Playback stop skipped: Missing mediaId or type.")
            return
        }
        
        if (duration <= 0 && type != "channel") { // Only require duration > 0 for non-live content
            Log.d("VideoPlayerVM", "Playback stop skipped: Invalid duration ($duration) for type $type.")
            return
        }

        // Simplification: We log the final position/duration, and rely on start_position_ms from playback_start
        // to calculate watch duration on the server side.
        val durationSeconds = if (duration > 0) duration / 1000 else 0
        val percentWatched = if (duration > 0) (position.toDouble() / duration.toDouble() * 100).toInt() else 0

        Log.d("VideoPlayerVM", "Logging playback_stop: ID=$id, Pos=$position, Dur=$duration")
        
        logAnalyticsEvent(
            eventName = "playback_stop",
            params = mapOf(
                "content_id" to id,
                "content_type" to type,
                "final_position_ms" to position.toString(),
                "duration_seconds" to durationSeconds.toString(),
                "watch_progress_percent" to percentWatched.toString()
            )
        )
    }
}

@Immutable
sealed class VideoPlayerUiState {
    data object Loading : VideoPlayerUiState()
    data class Ready(val movie: Movie, val startPositionMillis: Long, val isLive: Boolean = false) : VideoPlayerUiState()
    data object Error : VideoPlayerUiState()
}