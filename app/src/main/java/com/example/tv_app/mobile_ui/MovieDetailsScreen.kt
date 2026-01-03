package com.example.tv_app.mobile_ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.tv_app.R
import com.example.tv_app.model.Cast
import com.example.tv_app.model.Movie
import com.example.tv_app.model.MoviePalette
import com.example.tv_app.model.MovieReviewsAndRatings
import com.example.tv_app.presentation.common.MovieCard
import com.example.tv_app.presentation.common.TMDBPosterImage
import com.example.tv_app.presentation.utils.createVerticalBackgroundGradient
import com.example.tv_app.repository.EpgParserService
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.repository.TMDBService
import com.example.tv_app.repository.XtreamService
import kotlinx.coroutines.launch

// Define constant for fixed mobile padding
private val MobilePadding = 16.dp

@Composable
fun MovieDetailsScreen(
    key: Int = 0, // Key to force recomposition
    movie: Movie,
    playlistService: PlaylistService,
    onBackPressed: () -> Unit,
    onMovieSelected: (Movie) -> Unit = {},
    onPlayMovie: (Movie) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Force recomposition when key changes
    LaunchedEffect(key) {
        // This will trigger when key changes, ensuring fresh state
    }
    var movieDetails by remember { mutableStateOf<Movie?>(null) }
    var cast by remember { mutableStateOf<List<Cast>>(emptyList()) }
    var similarMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var genres by remember { mutableStateOf<List<String>>(emptyList()) }
    var reviewsAndRatings by remember { mutableStateOf<List<MovieReviewsAndRatings>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var posterUrl by remember { mutableStateOf<String?>(null) }
    var backdropUrl by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val tmdbService = remember { TMDBService() }
    val xtreamService = remember { XtreamService(EpgParserService()) }
    val tmdbImageProvider = remember { TMDBImageProvider.getInstance() }
    val lazyListState = rememberLazyListState()

    // Scroll to top when movie changes (similar movie selected)
    LaunchedEffect(key) {
        if (key > 0) { // Only scroll if key was changed (new movie selected)
            lazyListState.scrollToItem(0)
        }
    }

    LaunchedEffect(movie.tmdbId, movie.streamId) {
        isLoading = true
        coroutineScope.launch {
            if (movie.tmdbId.isNullOrBlank() || movie.tmdbId == "0") {
                // No TMDB ID - Fetch VOD info from Xtream API using stream_id and persist it locally
                val playlist = movie.playlist.target
                if (playlist != null) {
                    android.util.Log.d("MovieDetailsScreen", "Playlist found: ${playlist.name}. Attempting VOD info fetch.")
                    try {
                        val updatedMovie = playlistService.updateMovieInfo(movie, playlist)
                        
                        movieDetails = updatedMovie
                        
                        // Update local state from the now-persisted fields
                        posterUrl = updatedMovie.posterUrl ?: updatedMovie.coverUrl
                        backdropUrl = updatedMovie.backdropUrl ?: updatedMovie.posterUrl

                        // Map persistent castList to transient Cast model for UI display
                        cast = updatedMovie.castList?.map { name ->
                            Cast(name = name, profilePath = null, character = "")
                        } ?: emptyList()
                        
                        android.util.Log.d("MovieDetailsScreen", "VOD Info fetch successful. New description length: ${updatedMovie.description?.length}")

                    } catch (e: Exception) {
                        android.util.Log.e("MovieDetailsScreen", "VOD Info fetch failed for ${movie.streamId}", e)
                        movieDetails = movie
                        backdropUrl = movie.backdropUrl ?: movie.posterUrl
                    }
                } else {
                    android.util.Log.e("MovieDetailsScreen", "Error: Playlist is null for movie ${movie.name}")
                    movieDetails = movie
                    backdropUrl = movie.backdropUrl ?: movie.posterUrl
                }
                isLoading = false
                return@launch
            }

            try {
                movie.tmdbId?.let { tmdbId ->
                    val details = tmdbService.getMovieDetails(tmdbId)
                    details?.let {
                        movieDetails = movie.copy(
                            description = details.optString("overview", movie.description ?: ""),
                            rating = details.optDouble("vote_average", 0.0).toString(),
                            duration = details.optInt("runtime", 0).let { if (it > 0) "${it} min" else null },
                            trailer = details.optString("youtube_trailer", movie.trailer)
                        )
                        genres = tmdbService.parseGenres(details)
                        val images = tmdbService.getMovieImages(tmdbId, details)
                        posterUrl = images["poster"]
                        backdropUrl = images["backdrop"]
                    }
                    cast = tmdbService.getMovieCredits(tmdbId)
                    val tmdbSimilarMovies = tmdbService.getSimilarMovies(tmdbId)
                    // Cross-reference similar movies with local playlist
                    similarMovies = playlistService.crossReferenceSimilarMovies(tmdbSimilarMovies)
                    // Add mock reviews data similar to JetStreamCompose
                    reviewsAndRatings = listOf(
                        MovieReviewsAndRatings(
                            reviewerName = "FreshTomatoes",
                            reviewerIconUri = "",
                            reviewCount = "250",
                            reviewRating = "92%"
                        ),
                        MovieReviewsAndRatings(
                            reviewerName = "IMDb",
                            reviewerIconUri = "",
                            reviewCount = "1.2k",
                            reviewRating = "8.5"
                        )
                    )
                }
            } catch (e: Exception) {
                movieDetails = movie
                backdropUrl = movie.backdropUrl ?: movie.posterUrl
            } finally {
                isLoading = false
            }
        }
    }

    val displayMovie = movieDetails ?: movie
    val context = LocalContext.current
    var moviePalette by remember { mutableStateOf(MoviePalette()) }
    val posterForPalette = posterUrl ?: displayMovie.posterUrl

    LaunchedEffect(posterForPalette) {
        if (posterForPalette != null) {
            coroutineScope.launch {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(posterForPalette)
                    .allowHardware(false) // Important for Palette
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    try {
                        val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                            ?: result.drawable.toBitmap()

                        androidx.palette.graphics.Palette.from(bitmap).generate { palette ->
                            val dominantSwatch = palette?.dominantSwatch
                            val vibrantSwatch = palette?.vibrantSwatch
                            val darkVibrantSwatch = palette?.darkVibrantSwatch

                            moviePalette = MoviePalette(
                                background = dominantSwatch?.rgb?.let { Color(it) } ?: Color.Black,
                                primary = vibrantSwatch?.rgb?.let { Color(it) } ?: Color.White,
                                secondary = darkVibrantSwatch?.rgb?.let { Color(it) } ?: Color.LightGray,
                                tertiary = vibrantSwatch?.titleTextColor?.let { Color(it) } ?: Color.DarkGray
                            )
                        }
                    } catch (e: Exception) {
                        moviePalette = MoviePalette() // Fallback to default
                    }
                }
            }
        }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        else -> {
            Details(
                movieDetails = displayMovie,
                cast = cast,
                similarMovies = similarMovies,
                genres = genres,
                onPlayMovie = { onPlayMovie(displayMovie) },
                onBackPressed = onBackPressed,
                onMovieSelected = onMovieSelected,
                lazyListState = lazyListState,
                moviePalette = moviePalette,
                tmdbImageProvider = tmdbImageProvider,
                modifier = modifier
                    .fillMaxSize()
                    .animateContentSize()
            )
        }
    }
}

@Composable
private fun Details(
    movieDetails: Movie,
    cast: List<Cast>,
    similarMovies: List<Movie>,
    genres: List<String>,
    onPlayMovie: () -> Unit,
    onBackPressed: () -> Unit,
    onMovieSelected: (Movie) -> Unit,
    lazyListState: LazyListState,
    moviePalette: MoviePalette,
    tmdbImageProvider: TMDBImageProvider,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackPressed)

    // Use the dynamic gradient for the main screen background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = createVerticalBackgroundGradient(moviePalette))
    ) {
        // Main scrollable content
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(
                bottom = 60.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally // Center movie poster
        ) {
            // 1. Movie Poster Card
            item {
                MoviePosterCard(
                    movie = movieDetails,
                    tmdbImageProvider = tmdbImageProvider,
                    modifier = Modifier
                        .padding(top = 80.dp, bottom = 16.dp)
                        .width(160.dp) // Set width to match image ratio
                        .aspectRatio(1f / 1.5f) // Portrait aspect ratio
                )
            }

            // 2. Title
            item {
                Text(
                    text = movieDetails.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = MobilePadding)
                )
                // Display stream ID for debugging purposes
                Text(
                    text = "Stream ID: ${movieDetails.streamId}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = MobilePadding)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. Trailer Button (Bekijk trailer)
            if (!movieDetails.trailer.isNullOrBlank()) {
                item {
                    WatchTrailerButton(
                        trailerUrl = movieDetails.trailer!!,
                        modifier = Modifier.padding(horizontal = MobilePadding)
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 4. Metadata (Runtime, Genres, Rating icons)
            item {
                MetadataRowSmall(
                    movieDetails = movieDetails,
                    genres = genres,
                    modifier = Modifier.padding(horizontal = MobilePadding)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 5. Overview/Synopsis Title (Het verhaal)
            item {
                Text(
                    text = stringResource(id = R.string.overview),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MobilePadding)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 6. Overview/Synopsis Content
            item {
                Text(
                    text = movieDetails.description ?: stringResource(id = R.string.no_description_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    modifier = Modifier.padding(horizontal = MobilePadding)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 7. Cast and Crew List
            if (cast.isNotEmpty()) {
                item {
                    CastAndCrewList(
                        cast = cast,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }

            // 8. Similar Movies
            if (similarMovies.isNotEmpty()) {
                item {
                    MoviesRow(
                        title = stringResource(id = R.string.similar_movies),
                        movies = similarMovies,
                        onMovieSelected = onMovieSelected,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }
        }

        // Fixed elements overlay: Close button (Top Right) and Watch Movie (Bottom)

        // Close button (Top Right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 16.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onBackPressed),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Watch Movie Button (Fixed Bottom)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = MobilePadding, vertical = 8.dp) // Padding for visual spacing
        ) {
            WatchMovieButton(onClick = onPlayMovie)
        }
    }
}

@Composable
private fun MoviePosterCard(
    movie: Movie,
    tmdbImageProvider: TMDBImageProvider,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
    ) {
        TMDBPosterImage(
            tmdbId = movie.tmdbId,
            fallbackUrl = movie.posterUrl ?: movie.backdropUrl ?: movie.coverUrl,
            tmdbImageProvider = tmdbImageProvider,
            contentDescription = movie.name,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun WatchMovieButton(onClick: () -> Unit) {
    Button(
        onClick = { onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF007AFF), // Strong blue color
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.watch_movie),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun WatchTrailerButton(
    trailerUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            val youtubeUrl = "https://www.youtube.com/watch?v=$trailerUrl"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))
            context.startActivity(intent)
        },
        modifier = modifier
            .fillMaxWidth(0.6f)
            .height(40.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, Color.White),
        shape = RoundedCornerShape(20.dp) // Pill shape
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(id = R.string.watch_trailer),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun MetadataRowSmall(
    movieDetails: Movie,
    genres: List<String>,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        movieDetails.duration?.let { duration ->
            Text(
                text = duration,
                style = MaterialTheme.typography.labelMedium,
                color = Color.LightGray
            )
        }

        if (movieDetails.duration != null && genres.isNotEmpty()) {
            Text(
                text = "•",
                style = MaterialTheme.typography.labelMedium,
                color = Color.LightGray,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Text(
            text = genres.take(3).joinToString(" • "),
            style = MaterialTheme.typography.labelMedium,
            color = Color.LightGray
        )
    }
}

@Composable
private fun CastAndCrewList(cast: List<Cast>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(start = MobilePadding)) {
        Text(
            text = "Top Cast",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(end = MobilePadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cast.take(10), key = { "${it.name}-${it.character}" }) {
                CastAndCrewItem(it, modifier = Modifier.width(80.dp))
            }
            if (cast.size > 10) {
                item {
                    SeeAllCastButton(onClick = { /* TODO: Implement navigation */ })
                }
            }
        }
    }
}

@Composable
private fun SeeAllCastButton(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .height(144.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "See All Cast",
                tint = Color.White
            )
        }
        Text(
            text = "See All",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun CastAndCrewItem(
    castMember: Cast,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = {},
        modifier = modifier.aspectRatio(1 / 1.8f),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.5f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.725f)
            ) {
                AsyncImage(
                    model = castMember.profilePath?.let { TMDBService.getPosterUrl(it) },
                    contentDescription = castMember.name,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.275f)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = castMember.name,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = castMember.character,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MoviesRow(
    title: String,
    movies: List<Movie>,
    onMovieSelected: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(start = MobilePadding)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(end = MobilePadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies) { movie ->
                MovieCard(
                    movie = movie,
                    tmdbImageProvider = TMDBImageProvider.getInstance(),
                    onClick = { onMovieSelected(movie) },
                    modifier = Modifier.width(110.dp),
                    showTitle = true
                )
            }
        }
    }
}