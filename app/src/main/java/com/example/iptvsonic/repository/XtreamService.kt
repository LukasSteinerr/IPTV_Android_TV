package com.example.iptvsonic.repository

import com.example.iptvsonic.model.*
import com.example.iptvsonic.model.*
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
            requestTimeoutMillis = 30000 // 30 seconds
            connectTimeoutMillis = 10000  // 10 seconds
            socketTimeoutMillis = 10000   // 10 seconds
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
                lastModified = seriesObject["last_modified"]?.jsonPrimitive?.content,
                youtubeTrailer = seriesObject["youtube_trailer"]?.jsonPrimitive?.content
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
                                // Get episode number with fallback to 'episode' field
                                val episodeNum = episodeElement["episode_num"]?.jsonPrimitive
                                    ?: episodeElement["episode"]?.jsonPrimitive
                                val episodeNumber = episodeNum?.content?.toIntOrNull() ?: 0
                                
                                val titleElement = episodeElement["title"]?.jsonPrimitive
                                val title = titleElement?.content ?: "Episode $episodeNumber"
                                val containerExtension = episodeElement["container_extension"]?.jsonPrimitive?.content ?: "mp4"
                                
                                // Get stream ID with fallback to 'id' field
                                val idElement = episodeElement["id"]?.jsonPrimitive
                                val streamIdElement = episodeElement["stream_id"]?.jsonPrimitive
                                val streamId = idElement?.content ?: streamIdElement?.content
                                val streamUrl = if (streamId != null) {
                                    "$baseUrl/series/$username/$password/$streamId.$containerExtension"
                                } else ""
                                
                                // Initialize variables
                                var coverUrl: String? = null
                                var description: String? = null
                                var duration: String? = null
                                
                                // Debug logging for first episode of each season
                                if (episodeNumber == 1) {
                                    android.util.Log.d("XtreamService", "Episode data keys: ${episodeElement.keys}")
                                    android.util.Log.d("XtreamService", "Episode data: $episodeElement")
                                }
                                
                                // First check if episodeData contains 'info' and it's a Map (like in Flutter)
                                if (episodeElement.containsKey("info") && episodeElement["info"] is JsonObject) {
                                    val info = episodeElement["info"] as JsonObject
                                    
                                    // Debug logging for info object
                                    if (episodeNumber == 1) {
                                        android.util.Log.d("XtreamService", "Info object keys: ${info.keys}")
                                        android.util.Log.d("XtreamService", "Info object: $info")
                                    }
                                    
                                    // Extract movie_image from info (primary source as in Flutter)
                                    coverUrl = info["movie_image"]?.jsonPrimitive?.content
                                    
                                    // Extract plot from info
                                    description = info["plot"]?.jsonPrimitive?.content
                                    
                                    // Extract duration from info
                                    duration = info["duration"]?.jsonPrimitive?.content
                                }
                                
                                // If no cover from info, try the direct cover field
                                if (coverUrl == null) {
                                    coverUrl = episodeElement["cover"]?.jsonPrimitive?.content
                                }
                                
                                // If still no cover, use the series cover as fallback
                                if (coverUrl == null) {
                                    coverUrl = series.coverUrl
                                }
                                
                                // If no description from info, try other fields
                                if (description == null) {
                                    description = episodeElement["plot"]?.jsonPrimitive?.content
                                        ?: episodeElement["overview"]?.jsonPrimitive?.content
                                }
                                
                                // If no duration from info, try the direct duration field
                                if (duration == null) {
                                    duration = episodeElement["duration"]?.jsonPrimitive?.content
                                }
                                
                                // Debug logging the final cover URL
                                if (episodeNumber == 1) {
                                    android.util.Log.d("XtreamService", "Final cover URL for episode $episodeNumber: $coverUrl")
                                    android.util.Log.d("XtreamService", "Series cover URL: ${series.coverUrl}")
                                }
                                
                                val episode = TvEpisode(
                                    title = title,
                                    name = title,
                                    streamUrl = streamUrl,
                                    seasonNumber = seasonNumber,
                                    episodeNumber = episodeNumber,
                                    coverUrl = coverUrl,
                                    description = description,
                                    duration = duration,
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

    /**
     * Fetches and updates VOD movie info from the player API.
     * Used when a movie doesn't have a TMDB ID.
     * @param baseUrl The base URL of the Xtream service
     * @param username The username for authentication
     * @param password The password for authentication
     * @param movie The Movie object to update
     * @return The updated Movie object, or null if fetching failed.
     */
    suspend fun updateMovieVodInfo(baseUrl: String, username: String, password: String, movie: Movie): Movie? {
        val vodId = movie.streamId
        if (vodId == null) return null

        return try {
            val url = "$baseUrl/player_api.php?username=$username&password=$password&action=get_vod_info&vod_id=$vodId"
            val response: HttpResponse = client.get(url)
            val content = response.bodyAsText()
            android.util.Log.d("XtreamService", "Raw VOD Info response for vod_id $vodId: $content")
            val jsonObject = Json.parseToJsonElement(content).jsonObject
            
            // The details are nested under the "info" key
            val infoObject = jsonObject["info"]?.jsonObject ?: jsonObject
            
            // Update Movie fields directly from the response
            val fetchedDescription = infoObject["plot"]?.jsonPrimitive?.content
                ?: infoObject["description"]?.jsonPrimitive?.content
            
            android.util.Log.d("XtreamService", "VOD Info fetched description for ${movie.name}: $fetchedDescription")

            movie.description = fetchedDescription
            movie.backdropUrl = infoObject["backdrop_path"]?.jsonPrimitive?.content
            
            // Use movie_image as coverUrl/posterUrl for consistency
            val movieImage = infoObject["movie_image"]?.jsonPrimitive?.content
            movie.coverUrl = movieImage ?: movie.coverUrl
            movie.posterUrl = movieImage ?: movie.posterUrl
            
            movie.rating = infoObject["rating"]?.jsonPrimitive?.content
            movie.duration = infoObject["duration"]?.jsonPrimitive?.content
            movie.year = infoObject["releasedate"]?.jsonPrimitive?.content
            movie.trailer = infoObject["trailer"]?.jsonPrimitive?.content
            
            // Extract and map cast list to the new castList field
            val castList = mutableListOf<String>()
            infoObject["cast"]?.jsonPrimitive?.content?.split(",")?.forEach { name ->
                castList.add(name.trim())
            }
            if (castList.isNotEmpty()) {
                movie.castList = castList
            }

            movie
        } catch (e: Exception) {
            android.util.Log.e("XtreamService", "Failed to fetch VOD info for vod_id: $vodId", e)
            null
        }
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