package com.example.tv_app.repository

import android.content.Context
import android.os.Environment
import com.example.tv_app.model.DownloadedMovie
import com.example.tv_app.model.Movie
import com.example.tv_app.model.ObjectBox
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.readRemaining
import io.objectbox.Box
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import io.objectbox.kotlin.flow
import io.objectbox.query.QueryBuilder

class DownloadRepository(private val context: Context) {

    private val downloadBox: Box<DownloadedMovie> = ObjectBox.boxStore.boxFor(DownloadedMovie::class.java)
    private val client = HttpClient(CIO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val scope = CoroutineScope(Dispatchers.IO)

    // A shared flow to emit updates to UI if needed, though ObjectBox observers are usually enough.
    // However, fast progress updates might be better via Flow to avoid DB locking.
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
        val fileName = "${movie.name.replace("[^a-zA-Z0-9.-]".toRegex(), "_")}.mp4"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), fileName)
        
        val uniqueId = movie.streamId ?: movie.id.toString()

        // Check if already exists/downloading
        val existing = downloadBox.query()
            .equal(com.example.tv_app.model.DownloadedMovie_.movieId, uniqueId, QueryBuilder.StringOrder.CASE_SENSITIVE)
            .build()
            .findFirst()
        if (existing != null) {
            if (existing.status != DownloadedMovie.STATUS_COMPLETED && existing.status != DownloadedMovie.STATUS_DOWNLOADING) {
                 resumeDownload(existing.id)
            }
            return
        }

        val download = DownloadedMovie(
            movieId = uniqueId,
            movieName = movie.name,
            streamUrl = movie.streamUrl,
            posterUrl = movie.posterUrl ?: movie.coverUrl,
            backdropUrl = movie.backdropUrl ?: movie.posterUrl,
            localPath = file.absolutePath,
            status = DownloadedMovie.STATUS_DOWNLOADING,
            progress = 0,
            downloadedBytes = 0,
            totalBytes = 0
        )
        val id = downloadBox.put(download)
        download.id = id
        
        startDownloadJob(download, file)
    }

    private fun startDownloadJob(download: DownloadedMovie, file: File) {
        val job = scope.launch {
            try {
                // Determine start byte
                val startByte = if (file.exists()) file.length() else 0L
                download.downloadedBytes = startByte
                 // Update status to downloading
                download.status = DownloadedMovie.STATUS_DOWNLOADING
                downloadBox.put(download)
                _downloadUpdates.emit(download)

                val response: HttpResponse = client.get(download.streamUrl) {
                    if (startByte > 0) {
                        header(HttpHeaders.Range, "bytes=$startByte-")
                    }
                    onDownload { bytesSentTotal, contentLength ->
                        val total = contentLength ?: (download.totalBytes - startByte) // Approximation if unknown
                        // Note: contentLength here is the *remaining* length usually if range is used
                        
                        // We need to manage total carefully.
                        // If it's a range request, contentLength is the length of the chunk, not full file.
                        // If it's full request, it's full file.
                    }
                }

                if (!response.status.isSuccess()) {
                    throw Exception("Server returned ${response.status}")
                }
                
                val contentLength = response.headers[HttpHeaders.ContentLength]?.toLong() ?: -1L
                val contentRange = response.headers[HttpHeaders.ContentRange]
                
                // Calculate Total Size
                // If Content-Range: bytes 1000-4999/5000 -> Total is 5000
                // If no range, total is contentLength.
                
                var totalBytes = download.totalBytes
                if (contentRange != null) {
                     // Parse "bytes start-end/total"
                     val parts = contentRange.substringAfterLast("/").trim()
                     if (parts.all { it.isDigit() }) {
                         totalBytes = parts.toLong()
                     }
                } else if (totalBytes == 0L && contentLength != -1L) {
                    totalBytes = contentLength + startByte
                }
                
                download.totalBytes = totalBytes
                downloadBox.put(download)

                val channel: ByteReadChannel = response.body()
                val buffer = ByteArray(8 * 1024)
                
                // Use RandomAccessFile to append
                val raf = RandomAccessFile(file, "rw")
                raf.seek(startByte)

                var lastUpdate = System.currentTimeMillis()
                
                while (!channel.isClosedForRead) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read <= 0) break // EOF
                    raf.write(buffer, 0, read)
                    
                    download.downloadedBytes += read
                    
                    // Update progress
                    if (download.totalBytes > 0) {
                        download.progress = ((download.downloadedBytes.toDouble() / download.totalBytes) * 100).toInt()
                    }

                    // Throttle DB updates to 500ms
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 500) {
                        downloadBox.put(download)
                        _downloadUpdates.emit(download)
                        lastUpdate = now
                    }
                    
                    // Check for cancellation (handled by coroutine automatically via Job cancellationException, but we must ensure we catch clean up)
                }
                
                raf.close()
                
                if (download.downloadedBytes >= download.totalBytes && download.totalBytes > 0) {
                    download.status = DownloadedMovie.STATUS_COMPLETED
                    download.progress = 100
                    downloadBox.put(download)
                    _downloadUpdates.emit(download)
                }

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    // Paused manually
                    download.status = DownloadedMovie.STATUS_PAUSED
                } else {
                     e.printStackTrace()
                    download.status = DownloadedMovie.STATUS_FAILED
                }
                downloadBox.put(download)
                _downloadUpdates.emit(download)
            } finally {
                activeJobs.remove(download.id)
            }
        }
        activeJobs[download.id] = job
    }

    fun pauseDownload(id: Long) {
        activeJobs[id]?.cancel()
        // The catch block in startDownloadJob will handle the status update to PAUSED
    }

    fun resumeDownload(id: Long) {
        val download = downloadBox[id] ?: return
        if (download.status == DownloadedMovie.STATUS_COMPLETED) return
        
        val file = File(download.localPath ?: "")
        startDownloadJob(download, file)
    }

    fun deleteDownload(id: Long) {
        pauseDownload(id)
        val download = downloadBox[id] ?: return
        download.localPath?.let {
            val file = File(it)
            if (file.exists()) file.delete()
        }
        downloadBox.remove(id)
        // Emit deletion event (hacky: with id -1 or just handle list refresh in UI)
    }
    
    fun isDownloading(id: Long): Boolean {
        return activeJobs.containsKey(id)
    }
}
