package com.example.tv_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tv_app.model.Movie
import com.example.tv_app.model.TvEpisode
import com.example.tv_app.model.TvSeries
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.repository.TMDBService
import com.example.tv_app.repository.WatchProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.util.Log
import com.example.tv_app.model.WatchProgress

data class ContinueWatchingItem(
    val mediaId: String,
    val movie: Movie,
    val watchProgress: WatchProgress
)

class ContinueWatchingViewModel(
    private val playlistService: PlaylistService,
    private val tmdbService: TMDBService,
    private val watchProgressRepository: WatchProgressRepository
) : ViewModel() {

    private val _movieProgressItems = MutableStateFlow<List<ContinueWatchingItem>>(emptyList())
    val movieProgressItems: StateFlow<List<ContinueWatchingItem>> = _movieProgressItems.asStateFlow()

    private val _seriesProgressItems = MutableStateFlow<List<ContinueWatchingItem>>(emptyList())
    val seriesProgressItems: StateFlow<List<ContinueWatchingItem>> = _seriesProgressItems.asStateFlow()

    init {
        viewModelScope.launch {
            watchProgressRepository.getContinueWatchingFlow().collectLatest { progressList ->
                val allItems = progressList.map { progress ->
                    async(Dispatchers.IO) {
                        // mediaId format is generally "movie-ID" or "episode-ID" or a streamId
                        val (type, id) = parseMediaId(progress.mediaId)
                        
                        val movie: Movie? = when (type) {
                            "movie" -> playlistService.getMovieByStreamId(id)
                            "episode" -> playlistService.getTvEpisodeById(id)?.toMovieRepresentation(progress.mediaId)
                            else -> null
                        }
                        
                        if (movie != null) {
                            ContinueWatchingItem(
                                mediaId = progress.mediaId,
                                movie = movie,
                                watchProgress = progress
                            )
                        } else {
                            // If local media is missing, clean up the progress entry
                            Log.w("CWViewModel", "Local media not found for mediaId: ${progress.mediaId}. Deleting progress.")
                            watchProgressRepository.deleteProgress(progress.mediaId)
                            null
                        }
                    }
                }.awaitAll().filterNotNull() // Await all concurrent tasks and filter out nulls
                
                _movieProgressItems.value = allItems.filter { parseMediaId(it.mediaId).first == "movie" }
                _seriesProgressItems.value = allItems.filter { parseMediaId(it.mediaId).first == "episode" }
            }
        }
    }
    
    // Helper function to convert TvEpisode to a Movie object suitable for playback/display
    private fun TvEpisode.toMovieRepresentation(mediaId: String): Movie {
        return Movie(
            name = this.name.ifEmpty { this.title },
            streamUrl = this.streamUrl,
            description = this.description,
            duration = this.duration,
            streamId = mediaId, // Use the unique CW mediaId
            coverUrl = this.coverUrl
        )
    }

    private fun parseMediaId(mediaId: String): Pair<String, String> {
        return if (mediaId.startsWith("movie-")) {
            "movie" to mediaId.substringAfter("movie-")
        } else if (mediaId.startsWith("episode-")) {
            "episode" to mediaId.substringAfter("episode-")
        } else {
            // Fallback for streamId/tmdbId used directly
            "movie" to mediaId
        }
    }

    /**
     * Clears all movie/VOD watch progress entries in the repository.
     */
    fun clearMovieProgress() {
        watchProgressRepository.clearMovieProgress()
    }
    
    /**
     * Clears all TV series/episode watch progress entries in the repository.
     */
    fun clearSeriesProgress() {
        watchProgressRepository.clearSeriesProgress()
    }

    /**
     * Deletes a single watch progress entry by media ID.
     */
    fun deleteProgress(mediaId: String) {
        watchProgressRepository.deleteProgress(mediaId)
    }
}