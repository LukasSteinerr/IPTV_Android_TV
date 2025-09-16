package com.example.tv_app.repository

import android.util.Log
import com.example.tv_app.model.EpgChannelInfo
import com.example.tv_app.model.TvProgram
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class EpgParserService {

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 300_000 // 5 minutes
            connectTimeoutMillis = 60_000  // 1 minute
            socketTimeoutMillis = 60_000   // 1 minute
        }
    }

    data class EpgProgress(
        val processed: Int,
        val total: Int? = null,
        val phase: String = "Downloading"
    )

    suspend fun parseEpgData(
        url: String,
        batchSize: Int = 100, // Reduced batch size for better memory management
        maxPrograms: Int = 50_000, // Limit total programs to prevent memory issues
        onProgress: ((EpgProgress) -> Unit)? = null,
        onBatchReady: (List<TvProgram>, List<EpgChannelInfo>) -> Unit
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EpgParserService", "Starting EPG download from: $url")
                onProgress?.invoke(EpgProgress(0, null, "Connecting"))
                
                // Download with streaming to avoid loading entire file into memory
                val response: HttpResponse = client.get(url)
                val channel: ByteReadChannel = response.bodyAsChannel()
                
                Log.d("EpgParserService", "Download started, content length: ${response.headers["content-length"]}")
                onProgress?.invoke(EpgProgress(0, null, "Downloading"))
                
                // Read content in chunks to manage memory better
                val chunks = ArrayList<ByteArray>()
                var totalBytes = 0L
                val chunkSize = 8192 // 8KB chunks
                
                while (!channel.isClosedForRead) {
                    val chunk = ByteArray(chunkSize)
                    val bytesRead = channel.readAvailable(chunk, 0, chunkSize)
                    if (bytesRead == -1) break
                    
                    if (bytesRead < chunkSize) {
                        chunks.add(chunk.copyOf(bytesRead))
                    } else {
                        chunks.add(chunk)
                    }
                    totalBytes += bytesRead
                    
                    // Prevent excessive memory usage
                    if (totalBytes > 100_000_000) { // 100MB limit
                        Log.w("EpgParserService", "EPG file too large (${totalBytes / 1_000_000}MB), truncating")
                        break
                    }
                    
                    // Update progress every 1MB
                    if (totalBytes % 1_000_000 == 0L) {
                        onProgress?.invoke(EpgProgress((totalBytes / 1_000_000).toInt(), null, "Downloading"))
                    }
                }
                
                // Combine chunks into single byte array
                val xmlBytes = ByteArray(totalBytes.toInt())
                var offset = 0
                for (chunk in chunks) {
                    System.arraycopy(chunk, 0, xmlBytes, offset, chunk.size)
                    offset += chunk.size
                }
                chunks.clear() // Free memory
                
                Log.d("EpgParserService", "Download complete, parsing XML (${totalBytes / 1_000_000}MB)")
                onProgress?.invoke(EpgProgress(0, null, "Parsing"))
                
                // Parse XML with memory-efficient approach
                return@withContext parseXmlStream(
                    xmlBytes, 
                    batchSize, 
                    maxPrograms, 
                    onProgress, 
                    onBatchReady
                )
                
            } catch (e: Exception) {
                Log.e("EpgParserService", "Error downloading/parsing EPG", e)
                onProgress?.invoke(EpgProgress(0, null, "Error: ${e.message}"))
                false
            }
        }
    }

    private suspend fun parseXmlStream(
        xmlBytes: ByteArray,
        batchSize: Int,
        maxPrograms: Int,
        onProgress: ((EpgProgress) -> Unit)?,
        onBatchReady: (List<TvProgram>, List<EpgChannelInfo>) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            
            // Use InputStreamReader for better encoding handling
            val inputStream = ByteArrayInputStream(xmlBytes)
            val reader = InputStreamReader(inputStream, "UTF-8")
            parser.setInput(reader)

            var programs = ArrayList<TvProgram>(batchSize)
            var channels = ArrayList<EpgChannelInfo>(batchSize)
            var eventType = parser.eventType
            var currentProgram: TvProgram? = null
            var currentChannel: EpgChannelInfo? = null
            var text: String? = null
            var totalProcessed = 0
            var programsProcessed = 0

            val now = Calendar.getInstance().time
            val twentyFourHoursFromNow = Calendar.getInstance().apply { 
                add(Calendar.HOUR_OF_DAY, 24) 
            }.time

            while (eventType != XmlPullParser.END_DOCUMENT && programsProcessed < maxPrograms) {
                // Check for cancellation
                ensureActive()
                
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> when {
                        tagName.equals("programme", ignoreCase = true) -> {
                            val startTimeStr = parser.getAttributeValue(null, "start")
                            val startTime = parseEpgDate(startTimeStr)
                            
                            // Only process programs in the next 24 hours
                            if (startTime != null && startTime.after(now) && startTime.before(twentyFourHoursFromNow)) {
                                currentProgram = TvProgram().apply {
                                    channelXmlTvId = parser.getAttributeValue(null, "channel") ?: ""
                                    this.startTime = startTime
                                    this.stopTime = parseEpgDate(parser.getAttributeValue(null, "stop"))
                                }
                            } else {
                                currentProgram = null
                            }
                        }
                        tagName.equals("channel", ignoreCase = true) -> {
                            currentChannel = EpgChannelInfo().apply {
                                this.channelId = parser.getAttributeValue(null, "id") ?: ""
                            }
                        }
                        tagName.equals("icon", ignoreCase = true) -> {
                            currentChannel?.iconUrl = parser.getAttributeValue(null, "src")
                        }
                    }
                    XmlPullParser.TEXT -> {
                        text = parser.text?.takeIf { it.isNotBlank() }
                    }
                    XmlPullParser.END_TAG -> when {
                        tagName.equals("programme", ignoreCase = true) -> {
                            currentProgram?.let { 
                                programs.add(it)
                                programsProcessed++
                            }
                            currentProgram = null
                        }
                        tagName.equals("channel", ignoreCase = true) -> {
                            currentChannel?.let { 
                                if (it.channelId.isNotEmpty()) {
                                    channels.add(it) 
                                }
                            }
                            currentChannel = null
                        }
                        tagName.equals("title", ignoreCase = true) -> {
                            currentProgram?.title = text ?: ""
                        }
                        tagName.equals("desc", ignoreCase = true) -> {
                            currentProgram?.description = text
                        }
                        tagName.equals("display-name", ignoreCase = true) -> {
                            currentChannel?.displayName = text ?: ""
                        }
                    }
                }

                // Process batches when they reach the specified size
                if (programs.size >= batchSize || channels.size >= batchSize) {
                    if (programs.isNotEmpty() || channels.isNotEmpty()) {
                        onBatchReady(ArrayList(programs), ArrayList(channels))
                        totalProcessed += programs.size + channels.size
                        onProgress?.invoke(EpgProgress(totalProcessed, null, "Processing batch"))
                        programs.clear()
                        channels.clear()
                        
                        // Small delay to prevent UI blocking
                        delay(10)
                    }
                }

                eventType = parser.next()
            }
            
            // Process remaining items
            if (programs.isNotEmpty() || channels.isNotEmpty()) {
                onBatchReady(programs, channels)
                totalProcessed += programs.size + channels.size
            }
            
            Log.d("EpgParserService", "EPG parsing complete. Processed $programsProcessed programs and ${channels.size} channels")
            onProgress?.invoke(EpgProgress(totalProcessed, totalProcessed, "Complete"))
            
            true
        } catch (e: Exception) {
            Log.e("EpgParserService", "Error parsing XML", e)
            onProgress?.invoke(EpgProgress(0, null, "Parse Error: ${e.message}"))
            false
        }
    }

    private fun parseEpgDate(dateString: String?): Date? {
        if (dateString.isNullOrBlank()) return null
        
        return try {
            // Handle different EPG date formats
            when {
                dateString.contains("+") -> {
                    SimpleDateFormat("yyyyMMddHHmmss Z", Locale.getDefault()).parse(dateString)
                }
                dateString.length >= 14 -> {
                    SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).parse(dateString.substring(0, 14))
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.w("EpgParserService", "Failed to parse date: $dateString", e)
            null
        }
    }
    
    fun close() {
        client.close()
    }
}