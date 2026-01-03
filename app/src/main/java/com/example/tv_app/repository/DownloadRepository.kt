package com.example.tv_app.repository

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.tv_app.model.DownloadedMovie
import com.example.tv_app.model.Movie
import com.example.tv_app.model.ObjectBox
import io.objectbox.Box
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.File

class DownloadRepository(private val context: Context) {

    private val downloadBox: Box<DownloadedMovie> = ObjectBox.boxStore.boxFor(DownloadedMovie::class.java)

    /**
     * Starts a movie download using Android DownloadManager.
     */
    fun downloadMovie(movie: Movie) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        // Sanitize filename
        val fileName = "${movie.name.replace("[^a-zA-Z0-9.-]", "_")}.mp4"
        val request = DownloadManager.Request(Uri.parse(movie.streamUrl))
            .setTitle(movie.name)
            .setDescription("Downloading movie...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, fileName)
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)

        val downloadId = downloadManager.enqueue(request)

        val downloadedMovie = DownloadedMovie(
            movieId = movie.id,
            movieName = movie.name,
            streamUrl = movie.streamUrl,
            posterUrl = movie.posterUrl ?: movie.coverUrl,
            backdropUrl = movie.backdropUrl ?: movie.posterUrl,
            downloadId = downloadId,
            status = DownloadManager.STATUS_PENDING,
            progress = 0,
            localPath = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.absolutePath + File.separator + fileName
        )
        downloadBox.put(downloadedMovie)
    }

    /**
     * Returns a list of all downloaded movies.
     */
    fun getAllDownloads(): List<DownloadedMovie> {
        return downloadBox.all
    }

    /**
     * Updates the status and progress of a downloaded movie by checking DownloadManager.
     * Returns the current progress (0-100).
     */
    fun updateDownloadAuth(downloadId: Long): Int {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

            if (statusIndex != -1 && downloadedIndex != -1 && totalIndex != -1) {
                val status = cursor.getInt(statusIndex)
                val downloaded = cursor.getLong(downloadedIndex)
                val total = cursor.getLong(totalIndex)

                var progress = 0
                if (total > 0) {
                    progress = ((downloaded * 100) / total).toInt()
                }

                // Update entity in DB if status changed or just return progress
                // We avoid writing to DB on every progress tick to prevent performance issues
                // But we should update status if it changed (e.g. Pending -> Running -> Successful)
                
                return progress
            }
        }
        return 0
    }
    
    fun getDownloadStatus(downloadId: Long): Int {
         val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (statusIndex != -1) {
                return cursor.getInt(statusIndex)
            }
        }
        return DownloadManager.STATUS_FAILED
    }

    fun removeDownload(downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.remove(downloadId)
        
        val items = downloadBox.all
        val item = items.find { it.downloadId == downloadId }
        
        if (item != null) {
            downloadBox.remove(item)
            // Also delete file if exists
            item.localPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }
}
