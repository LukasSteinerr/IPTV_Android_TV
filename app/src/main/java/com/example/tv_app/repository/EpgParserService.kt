package com.example.tv_app.repository

import android.util.Log
import com.example.tv_app.model.EpgChannelInfo
import com.example.tv_app.model.TvProgram
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

open class EpgParserService {

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

    open suspend fun parseEpgData(
        url: String,
        onProgress: ((EpgProgress) -> Unit)? = null,
        onBatchReady: (List<TvProgram>, List<EpgChannelInfo>) -> Unit,
        onComplete: () -> Unit
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EpgParserService", "Starting EPG download from: $url")
                onProgress?.invoke(EpgProgress(0, null, "Connecting"))

                val response: HttpResponse = client.get(url)
                Log.d("EpgParserService", "Download started, content length: ${response.headers["content-length"]}")
                onProgress?.invoke(EpgProgress(0, null, "Downloading & Parsing"))

                // Parse XML directly from the stream
                val success = parseXmlStream(
                    response.bodyAsChannel().toInputStream(),
                    onProgress,
                    onBatchReady
                )
                if (success) {
                    onComplete()
                }
                return@withContext success

            } catch (e: Exception) {
                Log.e("EpgParserService", "Error downloading/parsing EPG", e)
                onProgress?.invoke(EpgProgress(0, null, "Error: ${e.message}"))
                false
            }
        }
    }

    private suspend fun parseXmlStream(
        inputStream: InputStream,
        onProgress: ((EpgProgress) -> Unit)?,
        onBatchReady: (List<TvProgram>, List<EpgChannelInfo>) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var programs = ArrayList<TvProgram>()
            var channels = ArrayList<EpgChannelInfo>()
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

            // Initial "Urgent" Batch
            val urgentBatchSize = 200
            var urgentBatchProcessed = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                ensureActive()

                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> when {
                        tagName.equals("programme", ignoreCase = true) -> {
                            val startTimeStr = parser.getAttributeValue(null, "start")
                            val startTime = parseEpgDate(startTimeStr)

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

                // Process urgent batch
                if (!urgentBatchProcessed && programs.size >= urgentBatchSize) {
                    onBatchReady(ArrayList(programs), ArrayList(channels))
                    val batchSize = programs.size + channels.size
                    totalProcessed += batchSize
                    onProgress?.invoke(EpgProgress(totalProcessed, null, "Processing urgent batch of $batchSize (Total: $totalProcessed)"))
                    programs.clear()
                    channels.clear()
                    urgentBatchProcessed = true
                }

                // Process dynamic batches in the background
                if (urgentBatchProcessed && (programs.size >= 4000 || channels.size >= 4000)) {
                    if (programs.isNotEmpty() || channels.isNotEmpty()) {
                        onBatchReady(ArrayList(programs), ArrayList(channels))
                        val batchSize = programs.size + channels.size
                        totalProcessed += batchSize
                        onProgress?.invoke(EpgProgress(totalProcessed, null, "Processing batch of $batchSize (Total: $totalProcessed)"))
                        programs.clear()
                        channels.clear()
                        delay(100) // Allow UI to update
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

    open fun close() {
        client.close()
    }
}