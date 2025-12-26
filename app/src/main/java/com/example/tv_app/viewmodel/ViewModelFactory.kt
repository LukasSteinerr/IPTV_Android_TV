package com.example.tv_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.repository.TMDBService

class SearchViewModelFactory(
    private val tmdbService: TMDBService,
    private val playlistService: PlaylistService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(tmdbService, playlistService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}