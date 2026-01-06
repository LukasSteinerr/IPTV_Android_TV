package com.example.iptvsonic.repository

import com.example.iptvsonic.model.Playlist
import com.example.iptvsonic.model.Channel
import com.example.iptvsonic.model.Category
import com.example.iptvsonic.model.ContentType
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.nio.charset.Charset

class M3uService {

    private val client = HttpClient(CIO)

    suspend fun parseM3uPlaylist(playlist: Playlist): Map<String, List<Any>> {
        try {
            val response: HttpResponse = client.get(playlist.url)
            val content = response.bodyAsText(Charset.forName("UTF-8"))
            return parseM3uContent(content, playlist)
        } catch (e: Exception) {
            throw Exception("Error parsing M3U playlist: $e")
        }
    }

    private fun parseM3uContent(content: String, playlist: Playlist): Map<String, List<Any>> {
        val lines = content.split('\n')

        if (lines.isEmpty() || !lines[0].trim().startsWith("#EXTM3U")) {
            throw Exception("Invalid M3U format")
        }

        val channels = mutableListOf<Channel>()
        val categories = mutableMapOf<String, Category>()

        var currentChannelName: String? = null
        var currentGroupTitle: String? = null
        var currentLogoUrl: String? = null
        var currentEpgId: String? = null

        for (i in 1 until lines.size) {
            val line = lines[i].trim()

            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF:")) {
                val infoLine = line.substring(line.indexOf(':') + 1)

                val nameMatch = Regex(",(.*)$").find(infoLine)
                currentChannelName = nameMatch?.groupValues?.get(1)?.trim()

                val groupMatch = Regex("group-title=\"([^\"]*)\"").find(infoLine)
                currentGroupTitle = groupMatch?.groupValues?.get(1)?.trim()

                val logoMatch = Regex("tvg-logo=\"([^\"]*)\"").find(infoLine)
                currentLogoUrl = logoMatch?.groupValues?.get(1)?.trim()

                val epgMatch = Regex("tvg-id=\"([^\"]*)\"").find(infoLine)
                currentEpgId = epgMatch?.groupValues?.get(1)?.trim()

            } else if (!line.startsWith('#') && currentChannelName != null) {
                val streamUrl = line

                if (currentGroupTitle != null && currentGroupTitle.isNotEmpty()) {
                    if (!categories.containsKey(currentGroupTitle)) {
                        val category = Category(
                            name = currentGroupTitle,
                            contentType = ContentType.liveTV
                        )
                        category.playlist.target = playlist
                        categories[currentGroupTitle] = category
                    }
                }

                val channel = Channel(
                    name = currentChannelName,
                    streamUrl = streamUrl,
                    logoUrl = currentLogoUrl,
                    epgId = currentEpgId
                )

                channel.playlist.target = playlist

                if (currentGroupTitle != null && currentGroupTitle.isNotEmpty()) {
                    channel.category.target = categories[currentGroupTitle]
                }

                channels.add(channel)

                currentChannelName = null
                currentLogoUrl = null
                currentEpgId = null
            }
        }

        return mapOf("channels" to channels, "categories" to categories.values.toList())
    }
}