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
    private val xtreamService = XtreamService()

    suspend fun getAllPlaylists(): List<Playlist> {
        return withContext(Dispatchers.IO) {
            playlistBox.all
        }
    }

    suspend fun addPlaylist(playlist: Playlist) {
        withContext(Dispatchers.IO) {
            if (playlist.isM3u) {
                val data = m3uService.parseM3uPlaylist(playlist)
                playlistBox.put(playlist)
                @Suppress("UNCHECKED_CAST")
                categoryBox.put(data["categories"] as List<Category>)
                @Suppress("UNCHECKED_CAST")
                channelBox.put(data["channels"] as List<Channel>)
            } else {
                tvProgramBox.removeAll()
                epgChannelInfoBox.removeAll()

                val data = xtreamService.fetchXtreamData(playlist)
                playlistBox.put(playlist)
                @Suppress("UNCHECKED_CAST")
                categoryBox.put(data["categories"] as List<Category>)
                @Suppress("UNCHECKED_CAST")
                channelBox.put(data["channels"] as List<Channel>)
                if (data.containsKey("movies")) {
                    @Suppress("UNCHECKED_CAST")
                    movieBox.put(data["movies"] as List<Movie>)
                }
                if (data.containsKey("series")) {
                    @Suppress("UNCHECKED_CAST")
                    tvSeriesBox.put(data["series"] as List<TvSeries>)
                }

                val baseUrl = xtreamService.getBaseUrl(playlist.url)
                val user = playlist.username ?: ""
                val pass = playlist.password ?: ""
                
                Log.d("PlaylistService", "Starting EPG data fetch for playlist: ${playlist.name}")
                
                val epgSuccess = xtreamService.fetchAndStoreEpgData(
                    baseUrl = baseUrl,
                    user = user,
                    pass = pass,
                    onProgress = { progress ->
                        Log.d("PlaylistService", "EPG Progress: ${progress.phase} - ${progress.processed}")
                    }
                ) { programs, channels ->
                    // Use database transactions for better performance and atomicity
                    try {
                        ensureActive() // Check for cancellation
                        
                        // Batch insert programs with transaction
                        if (programs.isNotEmpty()) {
                            ObjectBox.boxStore.runInTx {
                                tvProgramBox.put(programs)
                            }
                            Log.d("PlaylistService", "Stored ${programs.size} EPG programs")
                        }
                        
                        // Batch insert channels with transaction
                        if (channels.isNotEmpty()) {
                            ObjectBox.boxStore.runInTx {
                                epgChannelInfoBox.put(channels)
                            }
                            Log.d("PlaylistService", "Stored ${channels.size} EPG channels")
                        }
                    } catch (e: Exception) {
                        Log.e("PlaylistService", "Error storing EPG batch", e)
                    }
                }
                
                if (epgSuccess) {
                    Log.d("PlaylistService", "EPG data fetch completed successfully")
                } else {
                    Log.w("PlaylistService", "EPG data fetch failed or was incomplete")
                }
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
}