package com.example.tv_app.repository

import com.example.tv_app.model.*
import com.example.tv_app.model.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
                tmdbId = seriesObject["tmdb"]?.jsonPrimitive?.content
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