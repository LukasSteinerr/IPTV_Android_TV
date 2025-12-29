package com.example.tv_app.repository

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/// A service that provides TMDB images with caching
class TMDBImageProvider private constructor() {
    companion object {
        @Volatile
        private var INSTANCE: TMDBImageProvider? = null

        fun getInstance(): TMDBImageProvider {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TMDBImageProvider().also { INSTANCE = it }
            }
        }
    }

    private val tmdbService: TMDBService = TMDBService()

    // Cache for poster URLs - use distinct keys for movies and TV shows
    private val posterCache: ConcurrentHashMap<String, String?> = ConcurrentHashMap()

    // Cache for backdrop URLs - use distinct keys for movies and TV shows
    private val backdropCache: ConcurrentHashMap<String, String?> = ConcurrentHashMap()

    // Helper method to create distinct cache keys
    private fun createCacheKey(type: String, id: String?, isBackdrop: Boolean): String? {
        // Return null if id is null or empty to prevent invalid cache keys
        if (id.isNullOrEmpty()) {
            return null
        }
        return "${type}_${if (isBackdrop) "backdrop" else "poster"}_$id"
    }

    /**
     * Get a poster URL for a movie
     * Returns the fallback URL if TMDB ID is null or empty
     * or if the image is not yet loaded
     */
    suspend fun getPosterUrl(tmdbId: String?, fallbackUrl: String?): String? {
        // If no TMDB ID, return fallback
        if (tmdbId.isNullOrEmpty()) {
            return fallbackUrl
        }

        // Create a distinct cache key for movie posters
        val cacheKey = createCacheKey("movie", tmdbId, false)
        
        // Ensure cacheKey is not null
        if (cacheKey.isNullOrEmpty()) {
            return fallbackUrl
        }

        // Check cache first
        if (posterCache.containsKey(cacheKey)) {
            return posterCache[cacheKey] ?: fallbackUrl
        }

        // Not in cache, load it
        return try {
            val images = tmdbService.getMovieImages(tmdbId)
            val posterUrl = images["poster"]

            // Cache the result (even if null)
            posterCache[cacheKey] = posterUrl

            posterUrl ?: fallbackUrl
        } catch (e: Exception) {
            Log.e("TMDBImageProvider", "Error getting movie poster for TMDB ID: $tmdbId. Returning fallback: $fallbackUrl", e)
            // On error, return fallback
            fallbackUrl
        }
    }

    /**
     * Get a backdrop URL for a movie
     */
    suspend fun getBackdropUrl(tmdbId: String?): String? {
        // If no TMDB ID, return null
        if (tmdbId.isNullOrEmpty()) {
            return null
        }

        // Create a distinct cache key for movie backdrops
        val cacheKey = createCacheKey("movie", tmdbId, true)
        
        // Ensure cacheKey is not null
        if (cacheKey.isNullOrEmpty()) {
            return null
        }

        // Check cache first
        if (backdropCache.containsKey(cacheKey)) {
            return backdropCache[cacheKey]
        }

        // Not in cache, load it
        return try {
            val images = tmdbService.getMovieImages(tmdbId)
            val backdropUrl = images["backdrop"]

            // Cache the result (even if null)
            backdropCache[cacheKey] = backdropUrl

            backdropUrl
        } catch (e: Exception) {
            Log.e("TMDBImageProvider", "Error getting movie backdrop for TMDB ID: $tmdbId", e)
            null
        }
    }

    /**
     * Get a TV series poster URL
     */
    suspend fun getTvPosterUrl(tmdbId: String?, fallbackUrl: String?): String? {
        // If no TMDB ID, return fallback
        if (tmdbId.isNullOrEmpty()) {
            return fallbackUrl
        }

        // Create a distinct cache key for TV posters
        val cacheKey = createCacheKey("tv", tmdbId, false)
        
        // Ensure cacheKey is not null
        if (cacheKey.isNullOrEmpty()) {
            return fallbackUrl
        }

        // Check cache first
        if (posterCache.containsKey(cacheKey)) {
            return posterCache[cacheKey] ?: fallbackUrl
        }

        // Not in cache, load it
        return try {
            val images = tmdbService.getTvSeriesImages(tmdbId)
            val posterUrl = images["poster"]

            // Cache the result (even if null)
            posterCache[cacheKey] = posterUrl

            posterUrl ?: fallbackUrl
        } catch (e: Exception) {
            Log.e("TMDBImageProvider", "Error getting TV series poster for TMDB ID: $tmdbId. Returning fallback: $fallbackUrl", e)
            // On error, return fallback
            fallbackUrl
        }
    }

    /**
     * Get a TV series backdrop URL
     */
    suspend fun getTvBackdropUrl(tmdbId: String?): String? {
        // If no TMDB ID, return null
        if (tmdbId.isNullOrEmpty()) {
            return null
        }

        // Create a distinct cache key for TV backdrops
        val cacheKey = createCacheKey("tv", tmdbId, true)
        
        // Ensure cacheKey is not null
        if (cacheKey.isNullOrEmpty()) {
            return null
        }

        // Check cache first
        if (backdropCache.containsKey(cacheKey)) {
            return backdropCache[cacheKey]
        }

        // Not in cache, load it
        return try {
            val images = tmdbService.getTvSeriesImages(tmdbId)
            val backdropUrl = images["backdrop"]

            // Cache the result (even if null)
            backdropCache[cacheKey] = backdropUrl

            backdropUrl
        } catch (e: Exception) {
            Log.e("TMDBImageProvider", "Error getting TV series backdrop for TMDB ID: $tmdbId", e)
            null
        }
    }

    /**
     * Clear all caches
     */
    fun clearCache() {
        posterCache.clear()
        backdropCache.clear()
    }
}