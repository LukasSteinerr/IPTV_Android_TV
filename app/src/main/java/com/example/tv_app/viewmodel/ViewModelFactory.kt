package com.example.tv_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.repository.TMDBService

import com.example.tv_app.repository.WatchProgressRepository

class ViewModelFactory(
    private val tmdbService: TMDBService,
    private val playlistService: PlaylistService,
    private val watchProgressRepository: WatchProgressRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(tmdbService, playlistService) as T
        }
        if (modelClass.isAssignableFrom(ContinueWatchingViewModel::class.java)) {
            return ContinueWatchingViewModel(playlistService, tmdbService, watchProgressRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}