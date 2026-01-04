package com.example.tv_app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.tv_app.R
import com.example.tv_app.model.DownloadedMovie
import com.example.tv_app.model.ObjectBox
import com.example.tv_app.view.MainActivity
import io.objectbox.Box
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeDownloads = ConcurrentHashMap<Long, Job>()
    private lateinit var downloadBox: Box<DownloadedMovie>
    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val TAG = "DownloadService"
        private const val CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID = 1001
        private const val PROGRESS_UPDATE_INTERVAL = 1000L

        const val ACTION_START_DOWNLOAD = "com.example.tv_app.START_DOWNLOAD"
        const val ACTION_PAUSE_DOWNLOAD = "com.example.tv_app.PAUSE_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.example.tv_app.CANCEL_DOWNLOAD"
        const val EXTRA_DOWNLOAD_ID = "download_id"

        fun startDownload(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseDownload(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_PAUSE_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }

        fun cancelDownload(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        downloadBox = ObjectBox.boxStore.boxFor(DownloadedMovie::class.java)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val downloadId = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1L) ?: -1L

        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                if (downloadId != -1L) {
                    startDownloadTask(downloadId)
                }
            }
            ACTION_PAUSE_DOWNLOAD -> {
                if (downloadId != -1L) {
                    pauseDownloadTask(downloadId)
                }
            }
            ACTION_CANCEL_DOWNLOAD -> {
                if (downloadId != -1L) {
                    cancelDownloadTask(downloadId)
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_DEFAULT  // Changed from LOW to show in status bar
            ).apply {
                description = "Download progress notifications"
                setShowBadge(true)
                setSound(null, null)  // No sound for progress updates
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String, progress: Int = -1, movieName: String = ""): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (movieName.isNotEmpty()) movieName else "Downloading")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)  // Ensure visibility
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // Show on lock screen

        if (progress >= 0) {
            builder.setProgress(100, progress, false)
            builder.setSubText("$progress%")
        } else {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    private fun startDownloadTask(downloadId: Long) {
        if (activeDownloads.containsKey(downloadId)) {
            Log.d(TAG, "Download $downloadId already in progress")
            return
        }

        val download = downloadBox[downloadId]
        val movieName = download?.movieName ?: "Unknown"
        
        startForeground(NOTIFICATION_ID, createNotification("Starting download...", -1, movieName))

        val job = serviceScope.launch {
            if (download == null) {
                Log.e(TAG, "Download $downloadId not found")
                checkAndStopService()
                return@launch
            }

            try {
                performDownload(download)
            } catch (e: CancellationException) {
                Log.d(TAG, "Download $downloadId cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Download $downloadId failed: ${e.message}", e)
                download.status = DownloadedMovie.STATUS_FAILED
                downloadBox.put(download)
            } finally {
                activeDownloads.remove(downloadId)
                checkAndStopService()
            }
        }

        activeDownloads[downloadId] = job
    }

    private suspend fun performDownload(download: DownloadedMovie) {
        val file = File(download.localPath ?: return)
        val resumeFromBytes = if (file.exists()) {
            minOf(file.length(), download.downloadedBytes)
        } else {
            0L
        }

        Log.d(TAG, "Starting download: ${download.movieName} from byte $resumeFromBytes")

        val url = URL(download.streamUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
        if (resumeFromBytes > 0) {
            connection.setRequestProperty("Range", "bytes=$resumeFromBytes-")
        }
        connection.connectTimeout = 30000
        connection.readTimeout = 30000

        val responseCode = connection.responseCode
        Log.d(TAG, "Response code: $responseCode")

        if (responseCode != 200 && responseCode != 206) {
            throw Exception("HTTP error: $responseCode")
        }

        // If server doesn't support range and we had progress, start over
        if (responseCode == 200 && resumeFromBytes > 0) {
            Log.w(TAG, "Server doesn't support range requests, starting from beginning")
            file.delete()
        }

        val totalBytes = if (responseCode == 206) {
            // Partial content - total is content-range header or saved total
            download.totalBytes
        } else {
            connection.contentLengthLong.takeIf { it > 0 } ?: download.totalBytes
        }

        if (totalBytes > 0) {
            download.totalBytes = totalBytes
        }

        download.status = DownloadedMovie.STATUS_DOWNLOADING
        downloadBox.put(download)

        val inputStream = connection.inputStream
        val outputStream = java.io.FileOutputStream(file, responseCode == 206)

        val buffer = ByteArray(8192)
        var bytesRead: Int
        var totalBytesDownloaded = if (responseCode == 206) resumeFromBytes else 0L
        var lastUpdateTime = System.currentTimeMillis()

        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                // Check if cancelled
                if (!activeDownloads.containsKey(download.id)) {
                    Log.d(TAG, "Download ${download.id} was cancelled")
                    break
                }

                outputStream.write(buffer, 0, bytesRead)
                totalBytesDownloaded += bytesRead

                // Update progress periodically
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTime >= PROGRESS_UPDATE_INTERVAL) {
                    download.downloadedBytes = totalBytesDownloaded
                    if (download.totalBytes > 0) {
                        download.progress = ((totalBytesDownloaded.toDouble() / download.totalBytes) * 100).toInt()
                    }
                    downloadBox.put(download)

                    // Update notification
                    val notification = createNotification(
                        "Downloading... ${download.progress}%",
                        download.progress,
                        download.movieName
                    )
                    notificationManager.notify(NOTIFICATION_ID, notification)

                    Log.d(TAG, "Download progress: ${download.progress}% ($totalBytesDownloaded/${download.totalBytes})")
                    lastUpdateTime = currentTime
                }
            }
        } finally {
            outputStream.close()
            inputStream.close()
            connection.disconnect()
        }

        // Check final status
        if (activeDownloads.containsKey(download.id)) {
            download.downloadedBytes = totalBytesDownloaded
            if (download.totalBytes > 0 && totalBytesDownloaded >= download.totalBytes) {
                download.status = DownloadedMovie.STATUS_COMPLETED
                download.progress = 100
                Log.d(TAG, "Download completed: ${download.movieName}")
            }
            downloadBox.put(download)
        }
    }

    private fun pauseDownloadTask(downloadId: Long) {
        val job = activeDownloads.remove(downloadId)
        job?.cancel()

        val download = downloadBox[downloadId]
        if (download != null) {
            download.status = DownloadedMovie.STATUS_PAUSED
            downloadBox.put(download)
            Log.d(TAG, "Download paused: ${download.movieName}")
        }

        checkAndStopService()
    }

    private fun cancelDownloadTask(downloadId: Long) {
        val job = activeDownloads.remove(downloadId)
        job?.cancel()
        checkAndStopService()
    }

    private fun checkAndStopService() {
        if (activeDownloads.isEmpty()) {
            Log.d(TAG, "No active downloads, stopping service")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
}
