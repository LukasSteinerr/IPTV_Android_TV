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
import com.example.tv_app.service.DownloadService
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
                    // Download not found in DownloadManager
                    // Check if we have partial progress - if so, auto-resume with foreground service
                    if (download.downloadedBytes > 0 && download.totalBytes > 0 && download.downloadedBytes < download.totalBytes) {
                        Log.d(TAG, "Download ${download.movieName} has partial progress (${download.downloadedBytes}/${download.totalBytes}), auto-resuming via service...")
                        val file = File(download.localPath ?: "")
                        if (file.exists()) {
                            // Truncate file to match saved progress if needed
                            if (file.length() > download.downloadedBytes) {
                                try {
                                    java.io.RandomAccessFile(file, "rw").use { raf ->
                                        raf.setLength(download.downloadedBytes)
                                    }
                                    Log.d(TAG, "Truncated file to ${download.downloadedBytes} bytes")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to truncate file: ${e.message}")
                                }
                            }
                            // Resume via foreground service
                            DownloadService.startDownload(context, download.id)
                        } else {
                            // File doesn't exist, mark as paused so user can retry
                            Log.w(TAG, "Partial file missing for: ${download.movieName}")
                            download.status = DownloadedMovie.STATUS_PAUSED
                            downloadBox.put(download)
                        }
                    } else {
                        // No progress, mark as failed
                        Log.w(TAG, "Download not found in DownloadManager and no progress: ${download.movieName}")
                        download.status = DownloadedMovie.STATUS_FAILED
                        downloadBox.put(download)
                    }
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
                
                // Check if it's running in the foreground service
                if (download.status == DownloadedMovie.STATUS_DOWNLOADING) {
                    // Pause via service
                    DownloadService.pauseDownload(context, id)
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(context, "Download paused: ${download.movieName}", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                
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
        
        // First, get the current progress before removing from DownloadManager
        val download = downloadBox[id]
        if (download != null) {
            // Query current progress from DownloadManager before removing
            val query = DownloadManager.Query().setFilterById(downloadManagerId)
            val cursor = downloadManager.query(query)
            
            var bytesDownloaded = download.downloadedBytes
            var bytesTotal = download.totalBytes
            
            if (cursor != null && cursor.moveToFirst()) {
                val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                
                bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                bytesTotal = cursor.getLong(bytesTotalIndex)
                
                cursor.close()
            }
            
            // Save the progress before pausing
            download.downloadedBytes = bytesDownloaded
            download.totalBytes = bytesTotal
            if (bytesTotal > 0) {
                download.progress = ((bytesDownloaded.toDouble() / bytesTotal) * 100).toInt()
            }
            download.status = DownloadedMovie.STATUS_PAUSED
            downloadBox.put(download)
            
            Log.d(TAG, "Saving progress before pause: ${download.downloadedBytes}/${download.totalBytes} bytes (${download.progress}%)")
            
            // Remove from active jobs immediately so UI updates
            activeJobs.remove(id)
            
            scope.launch {
                _downloadUpdates.emit(download)
            }
            
            // Rename file to preserve it before DownloadManager deletes it (instant operation)
            val partialFile = File(download.localPath ?: "")
            val tempFile = File(partialFile.parent, "${partialFile.name}.partial")
            val dmId = downloadManagerId
            
            // Rename is instant (just changes file metadata, no data copy)
            if (partialFile.exists()) {
                val renamed = partialFile.renameTo(tempFile)
                Log.d(TAG, "Renamed partial file to temp: $renamed, size: ${tempFile.length()}")
            }
            
            // Remove from DownloadManager (this will try to delete the original file, but it's already renamed)
            val removed = downloadManager.remove(dmId)
            Log.d(TAG, "DownloadManager.remove returned: $removed for DM ID: $dmId")
            
            // Rename back to original name (also instant)
            if (tempFile.exists()) {
                val restored = tempFile.renameTo(partialFile)
                Log.d(TAG, "Restored partial file: $restored, size: ${partialFile.length()}")
            }
            
            Log.d(TAG, "Download paused: ${download.movieName} at ${download.downloadedBytes} bytes")
            
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
            val file = File(download.localPath ?: "")
            val fileSize = if (file.exists()) file.length() else 0L
            
            // Use the saved downloadedBytes from database as the source of truth
            val resumeFromBytes = download.downloadedBytes
            
            Log.d(TAG, "Resume check - file size: $fileSize, saved progress: $resumeFromBytes, total: ${download.totalBytes}")
            
            // If file is larger than what we recorded, truncate it to match our progress
            if (file.exists() && fileSize > resumeFromBytes && resumeFromBytes > 0) {
                Log.d(TAG, "File is larger than saved progress, truncating to $resumeFromBytes bytes")
                try {
                    java.io.RandomAccessFile(file, "rw").use { raf ->
                        raf.setLength(resumeFromBytes)
                    }
                    Log.d(TAG, "File truncated to ${file.length()} bytes")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to truncate file: ${e.message}")
                }
            }
            
            if (resumeFromBytes > 0 && download.totalBytes > 0 && resumeFromBytes < download.totalBytes) {
                // Use foreground service for background download with Range header support
                Log.d(TAG, "Resuming download via foreground service: ${download.movieName}")
                DownloadService.startDownload(context, id)
                
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "Resuming download: ${download.movieName}", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Start fresh download if no valid progress to resume
                Log.d(TAG, "No valid progress to resume (resumeFrom: $resumeFromBytes, total: ${download.totalBytes}), starting fresh download")
                // Delete any existing file to start fresh
                if (file.exists()) {
                    file.delete()
                }
                startFreshDownload(id, download, fileName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume download: ${e.message}", e)
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Failed to resume: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun startFreshDownload(id: Long, download: DownloadedMovie, fileName: String) {
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
        download.downloadedBytes = 0
        download.progress = 0
        downloadBox.put(download)
        scope.launch {
            _downloadUpdates.emit(download)
        }
        
        Log.d(TAG, "Starting fresh download: ${download.movieName} with DownloadManager ID: $downloadManagerId")
        
        scope.launch(Dispatchers.Main) {
            Toast.makeText(context, "Starting download: ${download.movieName}", Toast.LENGTH_SHORT).show()
        }
        
        monitorDownload(id, downloadManagerId)
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
