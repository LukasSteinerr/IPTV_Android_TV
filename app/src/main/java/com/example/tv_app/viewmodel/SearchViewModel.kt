package com.example.tv_app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tv_app.model.Movie
import com.example.tv_app.model.TvSeries
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.repository.TMDBService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SearchResult(
    val movies: List<Movie> = emptyList(),
    val tvShows: List<TvSeries> = emptyList()
)

class SearchViewModel(
    private val tmdbService: TMDBService,
    private val playlistService: PlaylistService
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow(SearchResult())
    val searchResults: StateFlow<SearchResult> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var searchJob: Job? = null

    fun updateSearchQuery(newQuery: String) {
        _searchQuery.value = newQuery
        searchJob?.cancel()
        
        if (newQuery.isBlank()) {
            _searchResults.value = SearchResult()
            _isLoading.value = false
            return
        }

        // Debounce mechanism: Wait for 500ms before executing the search
        searchJob = viewModelScope.launch {
            delay(500)
            executeSearch(newQuery)
        }
    }

private fun executeSearch(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Get the active playlist ID (using the first playlist found, as a fallback)
                val activePlaylistId = playlistService.getAllPlaylists().firstOrNull()?.id ?: run {
                    Log.w("SearchViewModel", "No active playlist found, skipping local search.")
                    _searchResults.value = SearchResult()
                    _isLoading.value = false
                    return@launch
                }

                // 2. Search local content (Movies, TV Series, Channels)
                val localResults = playlistService.searchContent(activePlaylistId, query)

                @Suppress("UNCHECKED_CAST")
                val localMovies = localResults["movies"] as? List<Movie> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val localTvSeries = localResults["series"] as? List<TvSeries> ?: emptyList()
                
                // 3. Search remote content (TMDB) for rich data/metadata - optional, removed from core logic to keep results streamlined to streamable content.
                // val remoteMovies = tmdbService.searchMovies(query)
                // val remoteTvSeries = tmdbService.searchTvSeries(query)
                
                // Aggregate local results into SearchResult.
                _searchResults.value = SearchResult(
                    movies = localMovies,
                    tvShows = localTvSeries
                )
                
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Search failed: ${e.message}", e)
                _searchResults.value = SearchResult()
            } finally {
                _isLoading.value = false
            }
        }
    }
}