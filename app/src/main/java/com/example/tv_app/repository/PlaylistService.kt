package com.example.tv_app.repository

import android.util.Log
import com.example.tv_app.model.ObjectBox
import com.example.tv_app.model.Playlist
import com.example.tv_app.model.Category
import com.example.tv_app.model.Channel
import com.example.tv_app.model.Movie
import com.example.tv_app.model.TvSeries
import com.example.tv_app.model.TvProgram
import com.example.tv_app.model.EpgChannelInfo
import com.example.tv_app.model.Category_
import com.example.tv_app.model.Movie_
import com.example.tv_app.model.Channel_
import com.example.tv_app.model.TvSeries_
import io.objectbox.Box
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive

class PlaylistService {

    private val playlistBox: Box<Playlist> = ObjectBox.boxStore.boxFor(Playlist::class.java)
    private val categoryBox: Box<Category> = ObjectBox.boxStore.boxFor(Category::class.java)
    private val channelBox: Box<Channel> = ObjectBox.boxStore.boxFor(Channel::class.java)
    private val movieBox: Box<Movie> = ObjectBox.boxStore.boxFor(Movie::class.java)
    private val tvSeriesBox: Box<TvSeries> = ObjectBox.boxStore.boxFor(TvSeries::class.java)
    private val tvProgramBox: Box<TvProgram> = ObjectBox.boxStore.boxFor(TvProgram::class.java)
    private val epgChannelInfoBox: Box<EpgChannelInfo> = ObjectBox.boxStore.boxFor(EpgChannelInfo::class.java)
    private val m3uService = M3uService()
    private val xtreamService = XtreamService(EpgParserService())
    private val tmdbService = TMDBService()
 
     suspend fun getAllPlaylists(): List<Playlist> {
         return withContext(Dispatchers.IO) {
            playlistBox.all
        }
    }

    suspend fun addPlaylist(playlist: Playlist, onProgress: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            if (playlist.isM3u) {
                onProgress("Parsing M3U playlist...")
                val data = m3uService.parseM3uPlaylist(playlist)
                onProgress("Storing M3U data...")
                playlistBox.put(playlist)
                @Suppress("UNCHECKED_CAST")
                categoryBox.put(data["categories"] as List<Category>)
                @Suppress("UNCHECKED_CAST")
                channelBox.put(data["channels"] as List<Channel>)
                onProgress("M3U playlist added successfully.")
            } else {
                onProgress("Clearing old EPG data...")
                tvProgramBox.removeAll()
                epgChannelInfoBox.removeAll()

                val data = xtreamService.fetchXtreamData(playlist, onProgress)
                onProgress("Storing playlist data...")
                playlistBox.put(playlist)

                val categories = data["categories"] as? List<Category>
                if (categories != null) {
                    onProgress("Storing ${categories.size} categories...")
                    categoryBox.put(categories)
                }

                val channels = data["channels"] as? List<Channel>
                if (channels != null) {
                    onProgress("Storing ${channels.size} live channels...")
                    channelBox.put(channels)
                }

                val movies = data["movies"] as? List<Movie>
                if (movies != null) {
                    onProgress("Storing ${movies.size} movies...")
                    movieBox.put(movies)
                    onProgress("Matching popular movies with your library...")
                    matchTmdbPopularMovies(playlist.id)
                }
 
                 val series = data["series"] as? List<TvSeries>
                 if (series != null) {
                    onProgress("Storing ${series.size} series...")
                    tvSeriesBox.put(series)
                    onProgress("Matching popular TV series with your library...")
                    matchTmdbPopularTvSeries(playlist.id)
                }
 
                 val baseUrl = xtreamService.getBaseUrl(playlist.url)
                val user = playlist.username ?: ""
                val pass = playlist.password ?: ""

                onProgress("Starting EPG data fetch...")
                Log.d("PlaylistService", "Starting EPG data fetch for playlist: ${playlist.name}")

                val remainingPrograms = mutableListOf<TvProgram>()
                val remainingEpgChannels = mutableListOf<EpgChannelInfo>()

                val epgSuccess = xtreamService.fetchAndStoreEpgData(
                    baseUrl = baseUrl,
                    user = user,
                    pass = pass,
                    onProgress = { progress ->
                        val progressMessage = "EPG: ${progress.phase} - Processed: ${progress.processed}"
                        onProgress(progressMessage)
                        Log.d("PlaylistService", progressMessage)
                    },
                    onBatchReady = { programs, epgChannels ->
                        try {
                            ensureActive()
                            if (programs.isNotEmpty() || epgChannels.isNotEmpty()) {
                                ObjectBox.boxStore.runInTx {
                                    if (programs.isNotEmpty()) {
                                        tvProgramBox.put(programs)
                                    }
                                    if (epgChannels.isNotEmpty()) {
                                        epgChannelInfoBox.put(epgChannels)
                                    }
                                }
                                val message = "Stored ${programs.size} EPG programs and ${epgChannels.size} channels."
                                onProgress(message)
                                Log.d("PlaylistService", message)
                            }
                        } catch (e: Exception) {
                            Log.e("PlaylistService", "Error storing EPG batch", e)
                            onProgress("Error storing EPG batch: ${e.message}")
                        }
                    },
                    onComplete = {
                        if (remainingPrograms.isNotEmpty() || remainingEpgChannels.isNotEmpty()) {
                            ObjectBox.boxStore.runInTx {
                                if (remainingPrograms.isNotEmpty()) {
                                    tvProgramBox.put(remainingPrograms)
                                }
                                if (remainingEpgChannels.isNotEmpty()) {
                                    epgChannelInfoBox.put(remainingEpgChannels)
                                }
                            }
                            val message = "Stored final batch of ${remainingPrograms.size} EPG programs and ${remainingEpgChannels.size} channels."
                            onProgress(message)
                            Log.d("PlaylistService", message)
                        }
                    }
                )
            }
        }
    }

    suspend fun deletePlaylist(id: Long) {
        withContext(Dispatchers.IO) {
            playlistBox.remove(id)
        }
    }

    suspend fun clearDatabase() {
        withContext(Dispatchers.IO) {
            ObjectBox.boxStore.removeAllObjects()
        }
    }

    suspend fun getCategoriesForPlaylist(playlistId: Long): List<Category> {
        return withContext(Dispatchers.IO) {
            categoryBox.query()
                .equal(Category_.playlistId, playlistId)
                .build()
                .find()
        }
    }

    suspend fun getMoviesForCategory(categoryId: Long): List<Movie> {
        return withContext(Dispatchers.IO) {
            movieBox.query()
                .equal(Movie_.categoryId, categoryId)
                .build()
                .find()
        }
    }

    suspend fun getChannelsForCategory(categoryId: Long): List<Channel> {
        return withContext(Dispatchers.IO) {
            channelBox.query()
                .equal(Channel_.categoryId, categoryId)
                .build()
                .find()
        }
    }

    suspend fun getTvSeriesForCategory(categoryId: Long): List<TvSeries> {
        return withContext(Dispatchers.IO) {
            tvSeriesBox.query()
                .equal(TvSeries_.categoryId, categoryId)
                .build()
                .find()
        }
    }

    private suspend fun getMoviesForPlaylist(playlistId: Long): List<Movie> {
        return withContext(Dispatchers.IO) {
            movieBox.query().equal(Movie_.playlistId, playlistId).build().find()
        }
    }

    private suspend fun getTvSeriesForPlaylist(playlistId: Long): List<TvSeries> {
        return withContext(Dispatchers.IO) {
            tvSeriesBox.query().equal(TvSeries_.playlistId, playlistId).build().find()
        }
    }

    private suspend fun matchTmdbPopularMovies(playlistId: Long) {
        try {
            val popularTmdbMovies = tmdbService.getPopularMovies()
            val localMovies = getMoviesForPlaylist(playlistId)
            val localMoviesByTmdbId = localMovies.filter { it.tmdbId != null && it.tmdbId!!.isNotEmpty() }
                .associateBy { it.tmdbId }

            val featuredMovies = mutableListOf<Movie>()
            for (tmdbMovie in popularTmdbMovies) {
                localMoviesByTmdbId[tmdbMovie.tmdbId]?.let { localMovie ->
                    localMovie.isFeatured = true
                    featuredMovies.add(localMovie)
                }
            }

            if (featuredMovies.isNotEmpty()) {
                movieBox.put(featuredMovies)
            }
        } catch (e: Exception) {
            Log.e("PlaylistService", "Error matching TMDB popular movies", e)
        }
    }

    private suspend fun matchTmdbPopularTvSeries(playlistId: Long) {
        try {
            val popularTmdbTvSeries = tmdbService.getPopularTvSeries()
            val localTvSeries = getTvSeriesForPlaylist(playlistId)
            val localTvSeriesByTmdbId = localTvSeries.filter { it.tmdbId != null && it.tmdbId!!.isNotEmpty() }
                .associateBy { it.tmdbId }

            val featuredTvSeries = mutableListOf<TvSeries>()
            for (tmdbSeries in popularTmdbTvSeries) {
                localTvSeriesByTmdbId[tmdbSeries.tmdbId]?.let { localSeries ->
                    localSeries.isFeatured = true
                    featuredTvSeries.add(localSeries)
                }
            }

            if (featuredTvSeries.isNotEmpty()) {
                tvSeriesBox.put(featuredTvSeries)
            }
        } catch (e: Exception) {
            Log.e("PlaylistService", "Error matching TMDB popular TV series", e)
        }
    }
}