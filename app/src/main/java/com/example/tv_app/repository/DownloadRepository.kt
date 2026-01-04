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

    init {
        // Restore active downloads when repository is created
        restoreActiveDownloads()
    }

    private val _downloadUpdates = MutableSharedFlow<DownloadedMovie>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val downloadUpdates = _downloadUpdates.asSharedFlow()
    
    private fun restoreActiveDownloads() {
        scope.launch {
            // Get all downloads that are in DOWNLOADING status
            val activeDownloads = downloadBox.query()
                .equal(com.example.tv_app.model.DownloadedMovie_.status, DownloadedMovie.STATUS_DOWNLOADING.toLong())
                .build()
                .find()
            
            Log.d(TAG, "Restoring ${activeDownloads.size} active downloads")
            
            // Query DownloadManager for all downloads
            val query = DownloadManager.Query()
            query.setFilterByStatus(
                DownloadManager.STATUS_RUNNING or 
                DownloadManager.STATUS_PENDING or 
                DownloadManager.STATUS_PAUSED
            )
            
            val cursor = downloadManager.query(query)
            val downloadManagerIds = mutableMapOf<String, Long>()
            
            if (cursor != null && cursor.moveToFirst()) {
                val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                val idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
                
                do {
                    val title = cursor.getString(titleIndex)
                    val dmId = cursor.getLong(idIndex)
                    downloadManagerIds[title] = dmId
                } while (cursor.moveToNext())
                
                cursor.close()
            }
            
            // Match our downloads with DownloadManager downloads
            for (download in activeDownloads) {
                val dmId = downloadManagerIds[download.movieName]
                if (dmId != null) {
                    activeJobs[download.id] = dmId
                    Log.d(TAG, "Restored download: ${download.movieName} with DM ID: $dmId")
                    // Start monitoring this download
                    monitorDownload(download.id, dmId)
                } else {
                    // Download not found in DownloadManager, mark as failed
                    Log.w(TAG, "Download not found in DownloadManager: ${download.movieName}")
                    download.status = DownloadedMovie.STATUS_FAILED
                    downloadBox.put(download)
                }
            }
        }
    }

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
                
                // Check if this download was removed from active jobs (paused/deleted)
                if (!activeJobs.containsKey(downloadId)) {
                    Log.d(TAG, "Download $downloadId removed from active jobs, stopping monitor")
                    break
                }
                
                val download = downloadBox[downloadId]
                if (download == null) {
                    Log.w(TAG, "Download $downloadId not found in database, stopping monitor")
                    break
                }
                
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
                    // Just stop monitoring - don't change status as it may have been paused
                    Log.d(TAG, "Download $downloadId not found in DownloadManager, stopping monitor")
                    isComplete = true
                }
            }
            Log.d(TAG, "Monitor stopped for download $downloadId")
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
        Log.d(TAG, "pauseDownload called for id: $id")
        Log.d(TAG, "Active jobs: ${activeJobs.keys}")
        
        val downloadManagerId = activeJobs[id]
        if (downloadManagerId == null) {
            Log.w(TAG, "Cannot pause: Download not found in active jobs. ID: $id")
            
            // Try to find it in DownloadManager by querying all downloads
            val download = downloadBox[id]
            if (download != null) {
                Log.d(TAG, "Found download in DB: ${download.movieName}, status: ${download.status}")
                
                // Query DownloadManager for this download
                val query = DownloadManager.Query()
                val cursor = downloadManager.query(query)
                
                if (cursor != null && cursor.moveToFirst()) {
                    val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                    val idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    
                    do {
                        val title = cursor.getString(titleIndex)
                        val dmId = cursor.getLong(idIndex)
                        val status = cursor.getInt(statusIndex)
                        
                        if (title == download.movieName) {
                            Log.d(TAG, "Found matching download in DownloadManager: $title, DM ID: $dmId, status: $status")
                            // Add to active jobs and try again
                            activeJobs[id] = dmId
                            cursor.close()
                            pauseDownload(id) // Recursive call now that we have the ID
                            return
                        }
                    } while (cursor.moveToNext())
                    
                    cursor.close()
                }
                
                Log.w(TAG, "Download not found in DownloadManager")
            }
            return
        }
        
        // Remove from DownloadManager (this cancels the download)
        val removed = downloadManager.remove(downloadManagerId)
        Log.d(TAG, "DownloadManager.remove returned: $removed for DM ID: $downloadManagerId")
        
        val download = downloadBox[id]
        if (download != null) {
            download.status = DownloadedMovie.STATUS_PAUSED
            downloadBox.put(download)
            scope.launch {
                _downloadUpdates.emit(download)
            }
            activeJobs.remove(id)
            Log.d(TAG, "Download paused: ${download.movieName}")
            
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Download paused: ${download.movieName}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun resumeDownload(id: Long) {
        val download = downloadBox[id]
        if (download == null) {
            Log.e(TAG, "Cannot resume: Download not found")
            return
        }
        
        if (download.status == DownloadedMovie.STATUS_COMPLETED) {
            Log.d(TAG, "Download already completed")
            return
        }
        
        if (!isNetworkAvailable()) {
            Log.e(TAG, "Cannot resume: No network connection")
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        // Check if already downloading
        if (activeJobs.containsKey(id)) {
            Log.d(TAG, "Download already in progress")
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Download already in progress", Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        try {
            val fileName = File(download.localPath ?: "").name
            
            // Note: Most IPTV servers don't support HTTP range requests (resume from middle)
            // So we restart the download from the beginning
            // Delete partial file if it exists
            val file = File(download.localPath ?: "")
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Deleted partial file for fresh start")
            }
            
            val request = DownloadManager.Request(Uri.parse(download.streamUrl))
                .setTitle(download.movieName)
                .setDescription("Resuming ${download.movieName}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
            
            val downloadManagerId = downloadManager.enqueue(request)
            activeJobs[id] = downloadManagerId
            
            // Reset progress since we're starting over
            download.downloadedBytes = 0
            download.progress = 0
            download.status = DownloadedMovie.STATUS_DOWNLOADING
            downloadBox.put(download)
            scope.launch {
                _downloadUpdates.emit(download)
            }
            
            Log.d(TAG, "Resuming download: ${download.movieName} with new DownloadManager ID: $downloadManagerId")
            
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Restarting download: ${download.movieName}", Toast.LENGTH_SHORT).show()
            }
            
            monitorDownload(id, downloadManagerId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume download: ${e.message}", e)
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Failed to resume: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
