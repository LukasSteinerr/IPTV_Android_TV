package com.example.tv_app.repository

import com.example.tv_app.model.*
import com.example.tv_app.model.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.contentOrNull

class XtreamService(private val epgParserService: EpgParserService) {

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 300_000 // 5 minutes
            connectTimeoutMillis = 60_000  // 1 minute
            socketTimeoutMillis = 60_000   // 1 minute
        }
    }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchXtreamData(playlist: Playlist, onProgress: (String) -> Unit): Map<String, List<Any>> {
        val baseUrl = getBaseUrl(playlist.url)
        val username = playlist.username ?: ""
        val password = playlist.password ?: ""

        onProgress("Fetching live TV data...")
        val liveData = fetchLiveData(baseUrl, username, password, playlist, onProgress)
        onProgress("Fetching movie data...")
        val movieData = fetchMovieData(baseUrl, username, password, playlist, onProgress)
        onProgress("Fetching series data...")
        val seriesData = fetchSeriesData(baseUrl, username, password, playlist, onProgress)

        val allCategories = (liveData["categories"] as List<Category>) +
                (movieData["categories"] as List<Category>) +
                (seriesData["categories"] as List<Category>)

        return mapOf(
            "channels" to liveData["channels"]!!,
            "categories" to allCategories,
            "movies" to movieData["movies"]!!,
            "series" to seriesData["series"]!!
        )
    }

    private suspend fun fetchLiveData(baseUrl: String, user: String, pass: String, playlist: Playlist, onProgress: (String) -> Unit): Map<String, List<Any>> {
        onProgress("Fetching live categories...")
        val categories = fetchCategories("$baseUrl/player_api.php?username=$user&password=$pass&action=get_live_categories", playlist, ContentType.liveTV)
        onProgress("Found ${categories.size} live categories. Fetching channels...")
        val channels = fetchLiveStreams("$baseUrl/player_api.php?username=$user&password=$pass&action=get_live_streams", baseUrl, user, pass, playlist, categories)
        onProgress("Found ${channels.size} live channels.")
        return mapOf("channels" to channels, "categories" to categories.values.toList())
    }

    private suspend fun fetchMovieData(baseUrl: String, user: String, pass: String, playlist: Playlist, onProgress: (String) -> Unit): Map<String, List<Any>> {
        onProgress("Fetching movie categories...")
        val categories = fetchCategories("$baseUrl/player_api.php?username=$user&password=$pass&action=get_vod_categories", playlist, ContentType.movie)
        onProgress("Found ${categories.size} movie categories. Fetching movies...")
        val movies = fetchMovieStreams("$baseUrl/player_api.php?username=$user&password=$pass&action=get_vod_streams", baseUrl, user, pass, playlist, categories)
        onProgress("Found ${movies.size} movies.")
        return mapOf("movies" to movies, "categories" to categories.values.toList())
    }

    private suspend fun fetchSeriesData(baseUrl: String, user: String, pass: String, playlist: Playlist, onProgress: (String) -> Unit): Map<String, List<Any>> {
        onProgress("Fetching series categories...")
        val categories = fetchCategories("$baseUrl/player_api.php?username=$user&password=$pass&action=get_series_categories", playlist, ContentType.series)
        onProgress("Found ${categories.size} series categories. Fetching series...")
        val series = fetchSeriesStreams("$baseUrl/player_api.php?username=$user&password=$pass&action=get_series", playlist, categories)
        onProgress("Found ${series.size} series.")
        return mapOf("series" to series, "categories" to categories.values.toList())
    }

    private suspend fun fetchCategories(url: String, playlist: Playlist, contentType: Int): Map<String, Category> {
        val response: HttpResponse = client.get(url)
        val content = response.bodyAsText()
        val jsonArray = Json.parseToJsonElement(content).jsonArray
        val categories = mutableMapOf<String, Category>()
        for (element in jsonArray) {
            val categoryObject = element.jsonObject
            val category = Category(
                name = categoryObject["category_name"]!!.jsonPrimitive.content,
                contentType = contentType
            )
            category.playlist.target = playlist
            categories[categoryObject["category_id"]!!.jsonPrimitive.content] = category
        }
        return categories
    }

    private suspend fun fetchLiveStreams(url: String, baseUrl: String, user: String, pass: String, playlist: Playlist, categories: Map<String, Category>): List<Channel> {
        val response: HttpResponse = client.get(url)
        val content = response.bodyAsText()
        val jsonArray = Json.parseToJsonElement(content).jsonArray
        val channels = mutableListOf<Channel>()
        for (element in jsonArray) {
            val channelObject = element.jsonObject
            val streamUrl = "$baseUrl/live/$user/$pass/${channelObject["stream_id"]!!.jsonPrimitive.content}.ts"
            val channel = Channel(
                name = channelObject["name"]!!.jsonPrimitive.content,
                streamUrl = streamUrl,
                logoUrl = channelObject["stream_icon"]?.jsonPrimitive?.content,
                epgId = channelObject["epg_channel_id"]?.jsonPrimitive?.content
            )
            channel.playlist.target = playlist
            val categoryId = channelObject["category_id"]?.jsonPrimitive?.content
            if (categoryId != null && categories.containsKey(categoryId)) {
                channel.category.target = categories[categoryId]
            }
            channels.add(channel)
        }
        return channels
    }

    private suspend fun fetchMovieStreams(url: String, baseUrl: String, user: String, pass: String, playlist: Playlist, categories: Map<String, Category>): List<Movie> {
        val response: HttpResponse = client.get(url)
        val content = response.bodyAsText()
        val jsonArray = Json.parseToJsonElement(content).jsonArray
        val movies = mutableListOf<Movie>()
        for (element in jsonArray) {
            val movieObject = element.jsonObject
            val containerExtension = movieObject["container_extension"]?.jsonPrimitive?.content ?: "mp4"
            val streamUrl = "$baseUrl/movie/$user/$pass/${movieObject["stream_id"]!!.jsonPrimitive.content}.$containerExtension"
            val movieInfo = movieObject["info"]?.jsonObject ?: movieObject
            val movie = Movie(
                name = movieObject["name"]!!.jsonPrimitive.content,
                streamUrl = streamUrl,
                coverUrl = movieObject["stream_icon"]?.jsonPrimitive?.content,
                description = movieInfo["plot"]?.jsonPrimitive?.content,
                year = movieInfo["releasedate"]?.jsonPrimitive?.content,
                duration = movieInfo["duration"]?.jsonPrimitive?.content,
                rating = movieInfo["rating"]?.jsonPrimitive?.content,
                streamId = movieObject["stream_id"]!!.jsonPrimitive.content,
                tmdbId = movieObject["tmdb"]?.jsonPrimitive?.content,
                trailer = movieInfo["trailer"]?.jsonPrimitive?.content ?: movieObject["trailer"]?.jsonPrimitive?.content,
                added = movieObject["added"]?.jsonPrimitive?.content,
                rating_5based = movieInfo["rating_5based"]?.jsonPrimitive?.content?.toDoubleOrNull()
            )
            movie.playlist.target = playlist
            val categoryId = movieObject["category_id"]?.jsonPrimitive?.content
            if (categoryId != null && categories.containsKey(categoryId)) {
                movie.category.target = categories[categoryId]
            }
            movies.add(movie)
        }
        return movies
    }

    private suspend fun fetchSeriesStreams(url: String, playlist: Playlist, categories: Map<String, Category>): List<TvSeries> {
        val response: HttpResponse = client.get(url)
        val content = response.bodyAsText()
        val jsonArray = Json.parseToJsonElement(content).jsonArray
        val seriesList = mutableListOf<TvSeries>()
        for (element in jsonArray) {
            val seriesObject = element.jsonObject
            val series = TvSeries(
                name = seriesObject["name"]!!.jsonPrimitive.content,
                coverUrl = seriesObject["cover"]?.jsonPrimitive?.content,
                seriesId = seriesObject["series_id"]!!.jsonPrimitive.content,
                tmdbId = seriesObject["tmdb"]?.jsonPrimitive?.content,
                lastModified = seriesObject["last_modified"]?.jsonPrimitive?.content
            )
            series.playlist.target = playlist
            val categoryId = seriesObject["category_id"]?.jsonPrimitive?.content
            if (categoryId != null && categories.containsKey(categoryId)) {
                series.category.target = categories[categoryId]
            }
            seriesList.add(series)
        }
        return seriesList
    }

    suspend fun fetchSeriesEpisodes(
        series: TvSeries,
        playlist: Playlist,
        onProgress: (String) -> Unit
    ): List<TvEpisode> {
        val baseUrl = getBaseUrl(playlist.url)
        val username = playlist.username ?: ""
        val password = playlist.password ?: ""
        
        onProgress("Fetching episodes for ${series.name}...")
        
        val url = "$baseUrl/player_api.php?username=$username&password=$password&action=get_series_info&series_id=${series.seriesId}"
        
        return try {
            val response: HttpResponse = client.get(url)
            val content = response.bodyAsText()
            val seriesInfoJson = Json.parseToJsonElement(content).jsonObject
            val episodes = mutableListOf<TvEpisode>()
            
            if (seriesInfoJson.containsKey("episodes")) {
                val episodesMap = seriesInfoJson["episodes"]?.jsonObject
                if (episodesMap == null) {
                    return emptyList()
                }
                
                episodesMap.forEach { (seasonKey, seasonData) ->
                    val seasonNumber = seasonKey.replace("season_", "").toIntOrNull() ?: 0
                    
                    if (seasonData is JsonArray) {
                        seasonData.forEach { episodeElement ->
                            if (episodeElement is JsonObject) {
                                val episodeNum = episodeElement["episode_num"]?.jsonPrimitive
                                val episodeNumber = episodeNum?.content?.toIntOrNull() ?: 0
                                val titleElement = episodeElement["title"]?.jsonPrimitive
                                val title = titleElement?.content ?: "Episode $episodeNumber"
                                val containerExtension = episodeElement["container_extension"]?.jsonPrimitive?.content ?: "mp4"
                                val idElement = episodeElement["id"]?.jsonPrimitive
                                val streamIdElement = episodeElement["stream_id"]?.jsonPrimitive
                                val streamId = idElement?.content ?: streamIdElement?.content
                                val streamUrl = if (streamId != null) {
                                    "$baseUrl/series/$username/$password/$streamId.$containerExtension"
                                } else ""
                                
                                val coverElement = episodeElement["cover"]?.jsonPrimitive
                                val plotElement = episodeElement["plot"]?.jsonPrimitive
                                val overviewElement = episodeElement["overview"]?.jsonPrimitive
                                val durationElement = episodeElement["duration"]?.jsonPrimitive
                                
                                val episode = TvEpisode(
                                    title = title,
                                    name = title,
                                    streamUrl = streamUrl,
                                    seasonNumber = seasonNumber,
                                    episodeNumber = episodeNumber,
                                    coverUrl = coverElement?.content ?: series.coverUrl,
                                    description = plotElement?.content ?: overviewElement?.content,
                                    duration = durationElement?.content,
                                    streamId = streamId
                                )
                                
                                episode.series.target = series
                                episodes.add(episode)
                            }
                        }
                    }
                }
            }
            
            onProgress("Found ${episodes.size} episodes for ${series.name}")
            episodes
        } catch (e: Exception) {
            android.util.Log.e("XtreamService", "Failed to fetch episodes for ${series.name}", e)
            onProgress("Failed to fetch episodes: ${e.message}")
            emptyList()
        }
    }

    internal fun getBaseUrl(url: String): String {
        val uri = java.net.URI(url)
        return "${uri.scheme}://${uri.host}:${uri.port}"
    }

    suspend fun fetchAndStoreEpgData(
        baseUrl: String,
        user: String,
        pass: String,
        onProgress: ((EpgParserService.EpgProgress) -> Unit)? = null,
        onBatchReady: (List<TvProgram>, List<EpgChannelInfo>) -> Unit,
        onComplete: () -> Unit
    ): Boolean {
        val epgUrl = "$baseUrl/xmltv.php?username=$user&password=$pass"
        return try {
            epgParserService.parseEpgData(
                url = epgUrl,
                onProgress = onProgress,
                onBatchReady = onBatchReady,
                onComplete = onComplete
            )
        } catch (e: Exception) {
            android.util.Log.e("XtreamService", "EPG fetch failed", e)
            onProgress?.invoke(EpgParserService.EpgProgress(0, null, "Failed: ${e.message}"))
            false
        } finally {
            epgParserService.close()
        }
    }
}