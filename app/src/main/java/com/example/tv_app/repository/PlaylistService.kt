package com.example.tv_app.repository

import com.example.tv_app.model.ObjectBox
import com.example.tv_app.model.Playlist
import com.example.tv_app.model.Category
import com.example.tv_app.model.Channel
import com.example.tv_app.model.Movie
import com.example.tv_app.model.TvSeries
import io.objectbox.Box
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaylistService {

    private val playlistBox: Box<Playlist> = ObjectBox.boxStore.boxFor(Playlist::class.java)
    private val categoryBox: Box<Category> = ObjectBox.boxStore.boxFor(Category::class.java)
    private val channelBox: Box<Channel> = ObjectBox.boxStore.boxFor(Channel::class.java)
    private val movieBox: Box<Movie> = ObjectBox.boxStore.boxFor(Movie::class.java)
    private val tvSeriesBox: Box<TvSeries> = ObjectBox.boxStore.boxFor(TvSeries::class.java)
    private val m3uService = M3uService()
    private val xtreamService = XtreamService()

    suspend fun getAllPlaylists(): List<Playlist> {
        return withContext(Dispatchers.IO) {
            playlistBox.all
        }
    }

    suspend fun addPlaylist(playlist: Playlist) {
        withContext(Dispatchers.IO) {
            val data = if (playlist.isM3u) {
                m3uService.parseM3uPlaylist(playlist)
            } else {
                xtreamService.fetchXtreamData(playlist)
            }
            playlistBox.put(playlist)
            categoryBox.put(data["categories"] as List<Category>)
            channelBox.put(data["channels"] as List<Channel>)
            if (data.containsKey("movies")) {
                movieBox.put(data["movies"] as List<Movie>)
            }
            if (data.containsKey("series")) {
                tvSeriesBox.put(data["series"] as List<TvSeries>)
            }
        }
    }

    suspend fun deletePlaylist(id: Long) {
        withContext(Dispatchers.IO) {
            playlistBox.remove(id)
        }
    }
}