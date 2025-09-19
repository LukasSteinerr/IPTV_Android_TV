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
import android.content.Context
import io.ktor.client.call.body
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
private data class CategoryDto(
    @SerialName("category_id") val categoryId: String,
    @SerialName("category_name") val categoryName: String
)

@Serializable
private data class LiveStreamDto(
    @SerialName("stream_id") val streamId: Int,
    val name: String,
    @SerialName("stream_icon") val streamIcon: String?,
    @SerialName("epg_channel_id") val epgChannelId: String?,
    @SerialName("category_id") val categoryId: String
)

@Serializable
private data class MovieStreamDto(
    @SerialName("stream_id") val streamId: Int,
    val name: String,
    @SerialName("stream_icon") val streamIcon: String?,
    @SerialName("rating_5based") val rating5based: Double?,
    val added: String?,
    @SerialName("category_id") val categoryId: String,
    @SerialName("container_extension") val containerExtension: String = "mp4"
)

@Serializable
private data class SeriesStreamDto(
    @SerialName("series_id") val seriesId: Int,
    val name: String,
    val cover: String?,
    val releaseDate: String?,
    val rating: String?,
    @SerialName("category_id") val categoryId: String
)

@Serializable
data class MovieInfoDto(
    val info: MovieInfoDetailsDto?,
    @SerialName("movie_data") val movieData: MovieDataDto?
)

@Serializable
data class MovieInfoDetailsDto(
    val plot: String?,
    val releasedate: String?,
    val duration: String?,
    val rating: String?,
    val trailer: String?
)

@Serializable
data class MovieDataDto(
    @SerialName("stream_id") val streamId: Int,
    val name: String,
    @SerialName("stream_icon") val streamIcon: String?,
    @SerialName("container_extension") val containerExtension: String = "mp4"
)

@Serializable
data class SeriesInfoDto(
    val info: SeriesInfoDetailsDto?,
    val episodes: Map<String, List<EpisodeDto>>? // Seasons are keys "1", "2", etc.
)

@Serializable
data class SeriesInfoDetailsDto(
    val name: String?,
    @SerialName("cover_big") val coverBig: String?,
    val plot: String?,
    val releaseDate: String?,
    val rating: String?
)

@Serializable
data class EpisodeDto(
    val id: String,
    val title: String?,
    @SerialName("container_extension") val containerExtension: String = "mp4",
    @SerialName("season") val seasonNumber: Int = 1
)

enum class StreamStatus {
    ONLINE, OFFLINE, MAYBE
}

class XtreamService(
    private val context: Context,
    private val epgParserService: EpgParserService
) {

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 300_000 // 5 minutes
            connectTimeoutMillis = 60_000  // 1 minute
            socketTimeoutMillis = 60_000   // 1 minute
        }
    }
    private val json = Json { ignoreUnknownKeys = true }

    private data class LiveDataResult(val channels: List<Channel>, val categories: List<Category>)
    private data class MovieDataResult(val movies: List<Movie>, val categories: List<Category>)
    private data class SeriesDataResult(val series: List<TvSeries>, val categories: List<Category>)

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

        val allCategories = liveData.categories + movieData.categories + seriesData.categories

        return mapOf(
            "channels" to liveData.channels,
            "categories" to allCategories,
            "movies" to movieData.movies,
            "series" to seriesData.series
        )
    }

    private suspend fun fetchLiveData(baseUrl: String, user: String, pass: String, playlist: Playlist, onProgress: (String) -> Unit): LiveDataResult {
        onProgress("Fetching live categories...")
        val categories = fetchAndCache(
            "live_categories_${playlist.id}.json",
            "$baseUrl/player_api.php?username=$user&password=$pass&action=get_live_categories"
        ) { dtos: List<CategoryDto> ->
            dtos.map { dto ->
                Category(name = dto.categoryName, contentType = ContentType.liveTV).apply {
                    this.playlist.target = playlist
                } to dto.categoryId
            }
        }.associate { it.second to it.first }

        onProgress("Found ${categories.size} live categories. Fetching channels...")
        val channels = fetchAndCache(
            "live_streams_${playlist.id}.json",
            "$baseUrl/player_api.php?username=$user&password=$pass&action=get_live_streams"
        ) { dtos: List<LiveStreamDto> ->
            dtos.map { dto ->
                Channel(
                    name = dto.name,
                    streamUrl = "$baseUrl/live/$user/$pass/${dto.streamId}.ts",
                    logoUrl = dto.streamIcon,
                    epgId = dto.epgChannelId
                ).apply {
                    this.playlist.target = playlist
                    this.category.target = categories[dto.categoryId]
                }
            }
        }
        onProgress("Found ${channels.size} live channels.")
        return LiveDataResult(channels, categories.values.toList())
    }

    private suspend fun fetchMovieData(baseUrl: String, user: String, pass: String, playlist: Playlist, onProgress: (String) -> Unit): MovieDataResult {
        onProgress("Fetching movie categories...")
        val categories = fetchAndCache(
            "movie_categories_${playlist.id}.json",
            "$baseUrl/player_api.php?username=$user&password=$pass&action=get_vod_categories"
        ) { dtos: List<CategoryDto> ->
            dtos.map { dto ->
                Category(name = dto.categoryName, contentType = ContentType.movie).apply {
                    this.playlist.target = playlist
                } to dto.categoryId
            }
        }.associate { it.second to it.first }

        onProgress("Found ${categories.size} movie categories. Fetching movies...")
        val movies = fetchAndCache(
            "movie_streams_${playlist.id}.json",
            "$baseUrl/player_api.php?username=$user&password=$pass&action=get_vod_streams"
        ) { dtos: List<MovieStreamDto> ->
            dtos.map { dto ->
                Movie(
                    name = dto.name,
                    streamUrl = "$baseUrl/movie/$user/$pass/${dto.streamId}.${dto.containerExtension}",
                    coverUrl = dto.streamIcon,
                    streamId = dto.streamId.toString(),
                    added = dto.added,
                    rating_5based = dto.rating5based
                ).apply {
                    this.playlist.target = playlist
                    this.category.target = categories[dto.categoryId]
                }
            }
        }
        onProgress("Found ${movies.size} movies.")
        return MovieDataResult(movies, categories.values.toList())
    }

    private suspend fun fetchSeriesData(baseUrl: String, user: String, pass: String, playlist: Playlist, onProgress: (String) -> Unit): SeriesDataResult {
        onProgress("Fetching series categories...")
        val categories = fetchAndCache(
            "series_categories_${playlist.id}.json",
            "$baseUrl/player_api.php?username=$user&password=$pass&action=get_series_categories"
        ) { dtos: List<CategoryDto> ->
            dtos.map { dto ->
                Category(name = dto.categoryName, contentType = ContentType.series).apply {
                    this.playlist.target = playlist
                } to dto.categoryId
            }
        }.associate { it.second to it.first }

        onProgress("Found ${categories.size} series categories. Fetching series...")
        val series = fetchAndCache(
            "series_streams_${playlist.id}.json",
            "$baseUrl/player_api.php?username=$user&password=$pass&action=get_series"
        ) { dtos: List<SeriesStreamDto> ->
            dtos.map { dto ->
                TvSeries(
                    name = dto.name,
                    coverUrl = dto.cover,
                    seriesId = dto.seriesId.toString(),
                    year = dto.releaseDate,
                    rating = dto.rating
                ).apply {
                    this.playlist.target = playlist
                    this.category.target = categories[dto.categoryId]
                }
            }
        }
        onProgress("Found ${series.size} series.")
        return SeriesDataResult(series, categories.values.toList())
    }

    private suspend inline fun <reified D, M> fetchAndCache(
        cacheFileName: String,
        url: String,
        crossinline mapper: (List<D>) -> List<M>
    ): List<M> {
        val cacheFile = File(context.cacheDir, cacheFileName)
        try {
            val response: HttpResponse = client.get(url)
            val content = response.bodyAsText()
            cacheFile.writeText(content) // Save fresh data to cache
            val dtos = json.decodeFromString<List<D>>(content)
            return mapper(dtos)
        } catch (e: Exception) {
            android.util.Log.w("XtreamService", "Failed to fetch from network, trying cache for $cacheFileName", e)
            if (cacheFile.exists()) {
                try {
                    val content = cacheFile.readText()
                    val dtos = json.decodeFromString<List<D>>(content)
                    return mapper(dtos)
                } catch (cacheEx: Exception) {
                    android.util.Log.e("XtreamService", "Failed to read or parse cache file $cacheFileName", cacheEx)
                }
            }
        }
        return emptyList()
    }

    suspend fun fetchMovieInfo(playlist: Playlist, vodId: String): MovieInfoDto? {
        return try {
            val baseUrl = getBaseUrl(playlist.url)
            val url = "$baseUrl/player_api.php?username=${playlist.username}&password=${playlist.password}&action=get_vod_info&vod_id=$vodId"
            val response: HttpResponse = client.get(url)
            response.body<MovieInfoDto>()
        } catch (e: Exception) {
            android.util.Log.e("XtreamService", "Failed to fetch movie info for VOD ID $vodId", e)
            null
        }
    }

    suspend fun fetchSeriesInfo(playlist: Playlist, seriesId: String): SeriesInfoDto? {
        return try {
            val baseUrl = getBaseUrl(playlist.url)
            val url = "$baseUrl/player_api.php?username=${playlist.username}&password=${playlist.password}&action=get_series_info&series_id=$seriesId"
            val response: HttpResponse = client.get(url)
            response.body<SeriesInfoDto>()
        } catch (e: Exception) {
            android.util.Log.e("XtreamService", "Failed to fetch series info for series ID $seriesId", e)
            null
        }
    }

    suspend fun checkStreamStatus(streamUrl: String): StreamStatus {
        return try {
            val response: HttpResponse = client.get(streamUrl) {
                timeout { requestTimeoutMillis = 7000 } // Use a short timeout
            }
            val content = response.bodyAsText()

            when {
                response.status.value != 200 -> StreamStatus.OFFLINE
                "offline" in content -> StreamStatus.OFFLINE
                "EXT-X-ENDLIST" in content -> StreamStatus.OFFLINE
                "#EXT-X-MEDIA-SEQUENCE:0" in content && "_1.ts" !in content -> StreamStatus.MAYBE
                else -> StreamStatus.ONLINE
            }
        } catch (e: Exception) {
            android.util.Log.d("XtreamService", "Stream check failed for $streamUrl: ${e.message}")
            StreamStatus.OFFLINE
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
        onBatchReady: (List<TvProgram>, List<EpgChannelInfo>) -> Unit
    ): Boolean {
        val epgUrl = "$baseUrl/xmltv.php?username=$user&password=$pass"
        return try {
            epgParserService.parseEpgData(
                url = epgUrl,
                onProgress = onProgress,
                onBatchReady = onBatchReady
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