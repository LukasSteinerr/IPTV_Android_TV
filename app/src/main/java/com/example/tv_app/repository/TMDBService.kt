package com.example.tv_app.repository

import android.util.Log
import com.example.tv_app.model.Cast
import com.example.tv_app.model.Movie
import com.example.tv_app.model.TvSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class TMDBService {
    companion object {
        private const val API_BASE_URL = "https://api.themoviedb.org/3"
        private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p"
        // TODO: Replace with your TMDB API key from https://www.themoviedb.org/settings/api
        private const val API_KEY = "9e92699e050cb40728b59728c3115455"

        private const val POSTER_SIZE = "w500"
        private const val FEATURED_POSTER_SIZE = "w780"
        private const val BACKDROP_SIZE = "w1280"

        fun getPosterUrl(path: String): String = "$IMAGE_BASE_URL/$POSTER_SIZE$path"
        fun getFeaturedPosterUrl(path: String): String = "$IMAGE_BASE_URL/$FEATURED_POSTER_SIZE$path"
        fun getBackdropUrl(path: String): String = "$IMAGE_BASE_URL/$BACKDROP_SIZE$path"
    }

    private suspend fun <T> safeApiCall(apiCall: suspend () -> T): T? {
        return try {
            apiCall()
        } catch (e: Exception) {
            Log.e("TMDBService", "API call failed", e)
            null
        }
    }

    private suspend fun fetchData(url: String): String? = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10000 // 10 seconds
            connection.readTimeout = 10000    // 10 seconds
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun getMovieDetails(tmdbId: String): JSONObject? = safeApiCall {
        val url = "$API_BASE_URL/movie/$tmdbId?api_key=$API_KEY"
        val response = fetchData(url)
        response?.let { JSONObject(it) }
    }

    suspend fun getTvSeriesDetails(tmdbId: String): JSONObject? = safeApiCall {
        val url = "$API_BASE_URL/tv/$tmdbId?api_key=$API_KEY"
        val response = fetchData(url)
        response?.let { JSONObject(it) }
    }

    suspend fun getMovieCredits(tmdbId: String): List<Cast> = safeApiCall {
        val url = "$API_BASE_URL/movie/$tmdbId/credits?api_key=$API_KEY"
        val response = fetchData(url)
        response?.let {
            val data = JSONObject(it)
            val castData = data.getJSONArray("cast")
            (0 until castData.length()).map { i ->
                val castObject = castData.getJSONObject(i)
                Cast(
                    name = castObject.optString("name", ""),
                    profilePath = castObject.optString("profile_path", null),
                    character = castObject.optString("character", "")
                )
            }
        } ?: emptyList()
    } ?: emptyList()


    suspend fun getMovieImages(tmdbId: String, details: JSONObject? = null): Map<String, String?> {
        val movieDetails = details ?: getMovieDetails(tmdbId)
        if (movieDetails == null) {
            return mapOf("poster" to null, "backdrop" to null)
        }

        val posterPath = movieDetails.optString("poster_path", null)
        val backdropPath = movieDetails.optString("backdrop_path", null)

        return mapOf(
            "poster" to posterPath?.let { getPosterUrl(it) },
            "backdrop" to backdropPath?.let { getBackdropUrl(it) }
        )
    }

    suspend fun getTvSeriesImages(tmdbId: String, details: JSONObject? = null): Map<String, String?> {
        val seriesDetails = details ?: getTvSeriesDetails(tmdbId)
        if (seriesDetails == null) {
            return mapOf("poster" to null, "backdrop" to null)
        }

        val posterPath = seriesDetails.optString("poster_path", null)
        val backdropPath = seriesDetails.optString("backdrop_path", null)

        return mapOf(
            "poster" to posterPath?.let { getPosterUrl(it) },
            "backdrop" to backdropPath?.let { getBackdropUrl(it) }
        )
    }

    suspend fun getPopularMovies(): List<Movie> = safeApiCall {
        val url = "$API_BASE_URL/movie/popular?api_key=$API_KEY"
        val response = fetchData(url)
        response?.let {
            val data = JSONObject(it)
            val results = data.getJSONArray("results")
            (0 until results.length()).map { i ->
                val movieData = results.getJSONObject(i)
                Movie(
                    name = movieData.optString("title", "No Title"),
                    streamUrl = "", // Placeholder
                    description = movieData.optString("overview", ""),
                    year = movieData.optString("release_date", "").take(4),
                    rating = movieData.optDouble("vote_average", 0.0).toString(),
                    tmdbId = movieData.optString("id"),
                    posterUrl = movieData.optString("poster_path")?.let { getPosterUrl(it) },
                    backdropUrl = movieData.optString("backdrop_path")?.let { getBackdropUrl(it) },
                    featuredPosterUrl = movieData.optString("poster_path")?.let { getFeaturedPosterUrl(it) },
                    streamId = movieData.optString("id"),
                    isFeatured = true
                )
            }
        } ?: emptyList()
    } ?: emptyList()

    suspend fun searchMovies(query: String): List<Movie> = safeApiCall {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$API_BASE_URL/search/movie?api_key=$API_KEY&query=$encodedQuery"
        val response = fetchData(url)
        response?.let {
            val data = JSONObject(it)
            val results = data.getJSONArray("results")
            (0 until results.length()).map { i ->
                val movieData = results.getJSONObject(i)
                Movie(
                    name = movieData.optString("title", "No Title"),
                    streamUrl = "", // Placeholder
                    description = movieData.optString("overview", ""),
                    year = movieData.optString("release_date", "").take(4),
                    rating = movieData.optDouble("vote_average", 0.0).toString(),
                    tmdbId = movieData.optString("id"),
                    posterUrl = movieData.optString("poster_path")?.let { getPosterUrl(it) },
                    backdropUrl = movieData.optString("backdrop_path")?.let { getBackdropUrl(it) },
                    featuredPosterUrl = movieData.optString("poster_path")?.let { getFeaturedPosterUrl(it) },
                    streamId = movieData.optString("id")
                )
            }
        } ?: emptyList()
    } ?: emptyList()
    
    suspend fun searchTvSeries(query: String): List<TvSeries> = safeApiCall {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$API_BASE_URL/search/tv?api_key=$API_KEY&query=$encodedQuery"
        val response = fetchData(url)
        response?.let {
            val data = JSONObject(it)
            val results = data.getJSONArray("results")
            (0 until results.length()).map { i ->
                val tvSeriesData = results.getJSONObject(i)
                TvSeries(
                    name = tvSeriesData.optString("name", "No Title"),
                    description = tvSeriesData.optString("overview", ""),
                    year = tvSeriesData.optString("first_air_date", "").take(4),
                    rating = tvSeriesData.optDouble("vote_average", 0.0).toString(),
                    tmdbId = tvSeriesData.optString("id"),
                    coverUrl = tvSeriesData.optString("poster_path")?.let { getPosterUrl(it) },
                    featuredPosterUrl = tvSeriesData.optString("poster_path")?.let { getFeaturedPosterUrl(it) }
                )
            }
        } ?: emptyList()
    } ?: emptyList()
    
    suspend fun getPopularTvSeries(): List<TvSeries> = safeApiCall {
        val url = "$API_BASE_URL/tv/popular?api_key=$API_KEY"
        val response = fetchData(url)
        response?.let {
            val data = JSONObject(it)
            val results = data.getJSONArray("results")
            (0 until results.length()).map { i ->
                val tvSeriesData = results.getJSONObject(i)
                TvSeries(
                    name = tvSeriesData.optString("name", "No Title"),
                    description = tvSeriesData.optString("overview", ""),
                    year = tvSeriesData.optString("first_air_date", "").take(4),
                    rating = tvSeriesData.optDouble("vote_average", 0.0).toString(),
                    tmdbId = tvSeriesData.optString("id"),
                    coverUrl = tvSeriesData.optString("poster_path")?.let { getPosterUrl(it) },
                    featuredPosterUrl = tvSeriesData.optString("poster_path")?.let { getFeaturedPosterUrl(it) },
                    isFeatured = true
                )
            }
        } ?: emptyList()
    } ?: emptyList()

    suspend fun getSimilarMovies(tmdbId: String): List<Movie> = safeApiCall {
        val url = "$API_BASE_URL/movie/$tmdbId/similar?api_key=$API_KEY"
        val response = fetchData(url)
        response?.let {
            val data = JSONObject(it)
            val results = data.getJSONArray("results")
            (0 until results.length()).map { i ->
                val movieData = results.getJSONObject(i)
                Movie(
                    name = movieData.optString("title", "No Title"),
                    streamUrl = "", // Placeholder
                    description = movieData.optString("overview", ""),
                    year = movieData.optString("release_date", "").take(4),
                    rating = movieData.optDouble("vote_average", 0.0).toString(),
                    tmdbId = movieData.optString("id"),
                    posterUrl = movieData.optString("poster_path")?.let { getPosterUrl(it) },
                    backdropUrl = movieData.optString("backdrop_path")?.let { getBackdropUrl(it) },
                    featuredPosterUrl = movieData.optString("poster_path")?.let { getFeaturedPosterUrl(it) },
                    streamId = movieData.optString("id")
                )
            }
        } ?: emptyList()
    } ?: emptyList()

    suspend fun getTvSeriesCredits(tmdbId: String): List<Cast> = safeApiCall {
        val url = "$API_BASE_URL/tv/$tmdbId/credits?api_key=$API_KEY"
        val response = fetchData(url)
        response?.let {
            val data = JSONObject(it)
            val castData = data.getJSONArray("cast")
            (0 until castData.length()).map { i ->
                val castObject = castData.getJSONObject(i)
                Cast(
                    name = castObject.optString("name", ""),
                    profilePath = castObject.optString("profile_path", null),
                    character = castObject.optString("character", "")
                )
            }
        } ?: emptyList()
    } ?: emptyList()

    suspend fun getSimilarTvSeries(tmdbId: String): List<TvSeries> = safeApiCall {
        val url = "$API_BASE_URL/tv/$tmdbId/similar?api_key=$API_KEY"
        val response = fetchData(url)
        response?.let {
            val data = JSONObject(it)
            val results = data.getJSONArray("results")
            (0 until results.length()).map { i ->
                val tvSeriesData = results.getJSONObject(i)
                TvSeries(
                    name = tvSeriesData.optString("name", "No Title"),
                    description = tvSeriesData.optString("overview", ""),
                    year = tvSeriesData.optString("first_air_date", "").take(4),
                    rating = tvSeriesData.optDouble("vote_average", 0.0).toString(),
                    tmdbId = tvSeriesData.optString("id"),
                    coverUrl = tvSeriesData.optString("poster_path")?.let { getPosterUrl(it) },
                    featuredPosterUrl = tvSeriesData.optString("poster_path")?.let { getFeaturedPosterUrl(it) }
                )
            }
        } ?: emptyList()
    } ?: emptyList()

    // Helper function to parse genres from TMDB response
    fun parseGenres(details: JSONObject): List<String> {
        return try {
            val genresArray = details.getJSONArray("genres")
            (0 until genresArray.length()).map { i ->
                genresArray.getJSONObject(i).getString("name")
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}