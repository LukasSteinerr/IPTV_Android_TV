package com.example.tv_app.repository

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.example.tv_app.model.DownloadedMovie
import com.example.tv_app.model.Movie
import com.example.tv_app.model.ObjectBox
import io.objectbox.Box
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import io.objectbox.kotlin.flow
import io.objectbox.query.QueryBuilder

class DownloadRepository(private val context: Context) {

    private val downloadBox: Box<DownloadedMovie> = ObjectBox.boxStore.boxFor(DownloadedMovie::class.java)
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val activeJobs = ConcurrentHashMap<Long, Long>() // downloadId -> DownloadManager requestId
    private val scope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "DownloadRepository"
        private const val PROGRESS_CHECK_INTERVAL = 1000L // Check progress every second
    }

    private val _downloadUpdates = MutableSharedFlow<DownloadedMovie>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val downloadUpdates = _downloadUpdates.asSharedFlow()

    fun getAllDownloadsFlow(): kotlinx.coroutines.flow.Flow<List<DownloadedMovie>> {
         return downloadBox.query().build().flow()
    }

    fun getAllDownloads(): List<DownloadedMovie> {
        return downloadBox.all
    }

    fun getDownload(id: Long): DownloadedMovie? = downloadBox[id]

    fun downloadMovie(movie: Movie) {
        Log.d(TAG, "=== DOWNLOAD MOVIE CALLED ===")
        Log.d(TAG, "Movie name: ${movie.name}")
        Log.d(TAG, "Stream URL: ${movie.streamUrl}")
        Log.d(TAG, "Stream ID: ${movie.streamId}")
        
        // Check network connectivity first
        if (!isNetworkAvailable()) {
            Log.e(TAG, "No network connection available")
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show()
            }
            return
        }
        
        Log.d(TAG, "Network is available")
        
        // Validate stream URL
        if (movie.streamUrl.isBlank()) {
            Log.e(TAG, "Invalid stream URL for movie: ${movie.name}")
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Invalid stream URL. Cannot download.", Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        Log.d(TAG, "Stream URL is valid: ${movie.streamUrl}")
        
        val fileName = "${movie.name.replace("[^a-zA-Z0-9.-]".toRegex(), "_")}.mkv"
        val uniqueId = movie.streamId ?: movie.id.toString()

        // Check if already exists/downloading
        val existing = downloadBox.query()
            .equal(com.example.tv_app.model.DownloadedMovie_.movieId, uniqueId, QueryBuilder.StringOrder.CASE_SENSITIVE)
            .build()
            .findFirst()
        if (existing != null) {
            if (existing.status != DownloadedMovie.STATUS_COMPLETED && existing.status != DownloadedMovie.STATUS_DOWNLOADING) {
                 resumeDownload(existing.id)
            } else {
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "Download already in progress or completed", Toast.LENGTH_SHORT).show()
                }
            }
            return
        }

        try {
            // Use Android DownloadManager for better reliability
            val request = DownloadManager.Request(Uri.parse(movie.streamUrl))
                .setTitle(movie.name)
                .setDescription("Downloading ${movie.name}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
            
            val downloadManagerId = downloadManager.enqueue(request)
            Log.d(TAG, "Download enqueued with DownloadManager ID: $downloadManagerId")
            
            val download = DownloadedMovie(
                movieId = uniqueId,
                movieName = movie.name,
                streamUrl = movie.streamUrl,
                posterUrl = movie.posterUrl ?: movie.coverUrl,
                backdropUrl = movie.backdropUrl ?: movie.posterUrl,
                localPath = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), fileName).absolutePath,
                status = DownloadedMovie.STATUS_DOWNLOADING,
                progress = 0,
                downloadedBytes = 0,
                totalBytes = 0
            )
            val id = downloadBox.put(download)
            download.id = id
            
            activeJobs[id] = downloadManagerId
            
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Download started: ${movie.name}", Toast.LENGTH_SHORT).show()
            }
            
            // Start monitoring the download progress
            monitorDownload(id, downloadManagerId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start download: ${e.message}", e)
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Failed to start download: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun monitorDownload(downloadId: Long, downloadManagerId: Long) {
        scope.launch {
            var isComplete = false
            while (!isComplete) {
                delay(PROGRESS_CHECK_INTERVAL)
                
                val download = downloadBox[downloadId] ?: break
                
                val query = DownloadManager.Query().setFilterById(downloadManagerId)
                val cursor: Cursor? = downloadManager.query(query)
                
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                    
                    val status = cursor.getInt(statusIndex)
                    val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                    val bytesTotal = cursor.getLong(bytesTotalIndex)
                    
                    download.downloadedBytes = bytesDownloaded
                    download.totalBytes = bytesTotal
                    
                    if (bytesTotal > 0) {
                        download.progress = ((bytesDownloaded.toDouble() / bytesTotal) * 100).toInt()
                    }
                    
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            download.status = DownloadedMovie.STATUS_COMPLETED
                            download.progress = 100
                            downloadBox.put(download)
                            _downloadUpdates.emit(download)
                            activeJobs.remove(downloadId)
                            isComplete = true
                            Log.d(TAG, "Download completed: ${download.movieName}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Download completed: ${download.movieName}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        DownloadManager.STATUS_FAILED -> {
                            val reason = cursor.getInt(reasonIndex)
                            val errorMsg = getDownloadErrorMessage(reason)
                            download.status = DownloadedMovie.STATUS_FAILED
                            downloadBox.put(download)
                            _downloadUpdates.emit(download)
                            activeJobs.remove(downloadId)
                            isComplete = true
                            Log.e(TAG, "Download failed: ${download.movieName}, reason: $errorMsg")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Download failed: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        }
                        DownloadManager.STATUS_PAUSED -> {
                            download.status = DownloadedMovie.STATUS_PAUSED
                            downloadBox.put(download)
                            _downloadUpdates.emit(download)
                        }
                        DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING -> {
                            download.status = DownloadedMovie.STATUS_DOWNLOADING
                            downloadBox.put(download)
                            _downloadUpdates.emit(download)
                            Log.d(TAG, "Download progress: ${download.progress}% (${download.downloadedBytes}/${download.totalBytes})")
                        }
                    }
                    cursor.close()
                } else {
                    // Download not found in DownloadManager
                    download.status = DownloadedMovie.STATUS_FAILED
                    downloadBox.put(download)
                    _downloadUpdates.emit(download)
                    activeJobs.remove(downloadId)
                    isComplete = true
                    Log.e(TAG, "Download not found in DownloadManager")
                }
            }
        }
    }
    
    private fun getDownloadErrorMessage(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume download"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "No external storage device found"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
            DownloadManager.ERROR_FILE_ERROR -> "Storage issue"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient storage space"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP response"
            DownloadManager.ERROR_UNKNOWN -> "Unknown error"
            else -> "Download error (code: $reason)"
        }
    }

    fun pauseDownload(id: Long) {
        val downloadManagerId = activeJobs[id] ?: return
        // DownloadManager doesn't support pause, so we cancel it
        downloadManager.remove(downloadManagerId)
        val download = downloadBox[id] ?: return
        download.status = DownloadedMovie.STATUS_PAUSED
        downloadBox.put(download)
        activeJobs.remove(id)
        Log.d(TAG, "Download paused: ${download.movieName}")
    }

    fun resumeDownload(id: Long) {
        val download = downloadBox[id] ?: return
        if (download.status == DownloadedMovie.STATUS_COMPLETED) return
        
        if (!isNetworkAvailable()) {
            Log.e(TAG, "Cannot resume: No network connection")
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        // For resume, we need to restart the download
        // DownloadManager will handle resume automatically if the server supports it
        try {
            val fileName = File(download.localPath ?: "").name
            val request = DownloadManager.Request(Uri.parse(download.streamUrl))
                .setTitle(download.movieName)
                .setDescription("Downloading ${download.movieName}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
            
            val downloadManagerId = downloadManager.enqueue(request)
            activeJobs[id] = downloadManagerId
            
            download.status = DownloadedMovie.STATUS_DOWNLOADING
            downloadBox.put(download)
            
            Log.d(TAG, "Resuming download: ${download.movieName}")
            monitorDownload(id, downloadManagerId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume download: ${e.message}", e)
        }
    }

    fun deleteDownload(id: Long) {
        val downloadManagerId = activeJobs[id]
        if (downloadManagerId != null) {
            downloadManager.remove(downloadManagerId)
            activeJobs.remove(id)
        }
        
        val download = downloadBox[id] ?: return
        download.localPath?.let {
            val file = File(it)
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Deleted file: ${file.absolutePath}")
            }
        }
        downloadBox.remove(id)
        Log.d(TAG, "Download deleted: ${download.movieName}")
    }
    
    fun isDownloading(id: Long): Boolean {
        return activeJobs.containsKey(id)
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }
}
