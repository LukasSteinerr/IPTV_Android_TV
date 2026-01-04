package com.example.tv_app.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import kotlinx.coroutines.launch
import java.io.File
import io.objectbox.kotlin.flow
import io.objectbox.query.QueryBuilder

class DownloadRepository(private val context: Context) {

    private val downloadBox: Box<DownloadedMovie> = ObjectBox.boxStore.boxFor(DownloadedMovie::class.java)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "DownloadRepository"
    }

    init {
        // Restore active downloads when repository is created
        restoreActiveDownloads()
    }
    
    private fun restoreActiveDownloads() {
        scope.launch {
            // Get all downloads that are in DOWNLOADING status
            val activeDownloads = downloadBox.query()
                .equal(com.example.tv_app.model.DownloadedMovie_.status, DownloadedMovie.STATUS_DOWNLOADING.toLong())
                .build()
                .find()
            
            Log.d(TAG, "Restoring ${activeDownloads.size} active downloads")
            
            for (download in activeDownloads) {
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
                } else if (download.downloadedBytes == 0L) {
                    // Fresh download that was interrupted, restart it
                    Log.d(TAG, "Restarting interrupted download: ${download.movieName}")
                    DownloadService.startDownload(context, download.id)
                } else {
                    // No valid progress, mark as failed
                    Log.w(TAG, "Download has invalid state: ${download.movieName}")
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
            // Create download entry in database
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
            
            Log.d(TAG, "Download entry created with ID: $id")
            
            // Start download via foreground service for consistent notification
            DownloadService.startDownload(context, id)
            
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Download started: ${movie.name}", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start download: ${e.message}", e)
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Failed to start download: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun pauseDownload(id: Long) {
        Log.d(TAG, "pauseDownload called for id: $id")
        
        val download = downloadBox[id]
        if (download == null) {
            Log.w(TAG, "Cannot pause: Download not found. ID: $id")
            return
        }
        
        Log.d(TAG, "Found download in DB: ${download.movieName}, status: ${download.status}")
        
        // Pause via service (service handles all downloads now)
        if (download.status == DownloadedMovie.STATUS_DOWNLOADING) {
            DownloadService.pauseDownload(context, id)
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
        
        if (download.status == DownloadedMovie.STATUS_DOWNLOADING) {
            Log.d(TAG, "Download already in progress")
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Download already in progress", Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        if (!isNetworkAvailable()) {
            Log.e(TAG, "Cannot resume: No network connection")
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        try {
            val file = File(download.localPath ?: "")
            val fileSize = if (file.exists()) file.length() else 0L
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
            
            // Start/resume download via foreground service
            Log.d(TAG, "Starting download via foreground service: ${download.movieName}")
            DownloadService.startDownload(context, id)
            
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Resuming download: ${download.movieName}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume download: ${e.message}", e)
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Failed to resume: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteDownload(id: Long) {
        // Cancel if running in service
        DownloadService.cancelDownload(context, id)
        
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
