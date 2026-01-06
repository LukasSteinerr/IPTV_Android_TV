package com.example.tv_app.mobile_ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder // Add import for SvgDecoder
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.tv_app.R
import com.example.tv_app.model.Cast
import com.example.tv_app.model.Movie
import com.example.tv_app.model.MoviePalette
import com.example.tv_app.model.MovieReviewsAndRatings
import com.example.tv_app.model.ObjectBox
import com.example.tv_app.presentation.common.MovieCard
import com.example.tv_app.presentation.common.TMDBPosterImage
import com.example.tv_app.presentation.components.LoadingIndicator
import com.example.tv_app.presentation.utils.createVerticalBackgroundGradient
import com.example.tv_app.repository.EpgParserService
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.repository.TMDBService
import com.example.tv_app.repository.XtreamService
import com.example.tv_app.cast.CastHelper // Import CastHelper
import com.example.tv_app.presentation.components.FullScreenDarkLoading // Import shared loading screen
import com.example.tv_app.repository.WatchProgressRepository
import com.example.tv_app.presentation.utils.parseDurationToMillis
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
    onDownloadMovie: (Movie) -> Unit = {},
    onMyListToggled: () -> Unit = {}, // New callback
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
    var isLiked by remember(movie.id) { mutableStateOf(movie.myList == 1) }

    val coroutineScope = rememberCoroutineScope()
    val tmdbService = remember { TMDBService() }
    val xtreamService = remember { XtreamService(EpgParserService()) }
    val tmdbImageProvider = remember { TMDBImageProvider.getInstance() }
    val watchProgressRepository = remember { WatchProgressRepository() }
    val lazyListState = rememberLazyListState()
    
    // State for watch progress display
    var watchProgressPercent by remember { mutableStateOf(0f) }

    // Scroll to top when movie changes (similar movie selected)
    LaunchedEffect(key) {
        if (key > 0) { // Only scroll if key was changed (new movie selected)
            lazyListState.scrollToItem(0)
        }
    }

    LaunchedEffect(movie.tmdbId, movie.streamId) {
        isLoading = true
        
        // Helper function to update progress state
        fun updateProgress() {
            val mediaId = movie.streamId ?: if (movie.id > 0) "movie-${movie.id}" else null
            
            if (mediaId != null) {
                // Ensure movieDetails has been fetched, otherwise use fallback movie duration
                val durationMillis = parseDurationToMillis(movieDetails?.duration ?: movie.duration)
                val positionMillis = watchProgressRepository.getSavedPosition(mediaId)
                
                watchProgressPercent = if (durationMillis > 0 && positionMillis > 0) {
                    (positionMillis.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
                } else 0f
            } else {
                watchProgressPercent = 0f
            }
        }
        
        // Run network/DB updates
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
                updateProgress()
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
                            trailer = details.optString("youtube_trailer", movie.trailer ?: "")
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
                updateProgress()
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
            FullScreenDarkLoading(modifier = Modifier.fillMaxSize())
        }
        else -> {
            Details(
                movieDetails = displayMovie,
                cast = cast,
                similarMovies = similarMovies,
                genres = genres,
                watchProgressPercent = watchProgressPercent, // Pass new state here
                isLiked = isLiked,
                onPlayMovie = {
                    // 1. Check for active Cast session and attempt to cast
                    val castSucceeded = CastHelper.startCasting(context, displayMovie)

                    // 2. If casting failed (no session or error), launch local player
                    if (!castSucceeded) {
                        onPlayMovie(displayMovie)
                    }
                },
                onDownloadMovie = { onDownloadMovie(displayMovie) },
                onToggleMyList = { targetMovie ->
                    // Immediate UI Update
                    isLiked = !isLiked
                    coroutineScope.launch {
                        val movieBox = ObjectBox.boxStore.boxFor(Movie::class.java)
                        val dbMovie = movieBox.get(targetMovie.id)
                        val newStatus = if (isLiked) 1 else 0

                        if (dbMovie != null) {
                            dbMovie.myList = newStatus
                            movieBox.put(dbMovie)
                        }
                        // Update the transient object too to keep consistency if needed elsewhere
                        targetMovie.myList = newStatus
                        // If we have detailed info, update it too
                        if (movieDetails != null) {
                           movieDetails = movieDetails!!.copy(myList = newStatus)
                        }
                        // Notify MainActivity that MyList status changed
                        onMyListToggled()
                    }
                },
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
    watchProgressPercent: Float,
    isLiked: Boolean,
    onPlayMovie: () -> Unit,
    onDownloadMovie: () -> Unit,
    onToggleMyList: (Movie) -> Unit,
    onBackPressed: () -> Unit,
    onMovieSelected: (Movie) -> Unit,
    lazyListState: LazyListState,
    moviePalette: MoviePalette,
    tmdbImageProvider: TMDBImageProvider,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackPressed)

    // Set a solid black background color for the entire screen.
    // The palette-based gradient will be applied internally to the header content so it scrolls away.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume clicks to prevent them from passing to screens below */ }
            .then(modifier)
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
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Apply the palette gradient to the top content area so it scrolls away
                        .background(brush = createVerticalBackgroundGradient(moviePalette)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Movie Poster Card
                    MoviePosterCard(
                        movie = movieDetails,
                        tmdbImageProvider = tmdbImageProvider,
                        progressPercent = watchProgressPercent, // Pass progress
                        onPlayClick = onPlayMovie, // Pass the play action
                        modifier = Modifier
                            .padding(top = 80.dp, bottom = 16.dp)
                            .width(160.dp) // Set width to match image ratio
                            .aspectRatio(1f / 1.5f) // Portrait aspect ratio
                    )

                    // 2. Title
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

                    // 3. Action Buttons (Trailer and Download)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MobilePadding),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Trailer button only shows if trailer exists
                        if (!movieDetails.trailer.isNullOrBlank()) {
                            WatchTrailerButton(
                                trailerUrl = movieDetails.trailer!!,
                                modifier = Modifier // Uses internal fillMaxWidth(0.6f) relative to this Row
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        DownloadButton(
                            onClick = onDownloadMovie
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        HeartButton(
                            isLiked = isLiked,
                            onClick = { onToggleMyList(movieDetails) }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Metadata (Runtime, Genres, Rating icons)
                    MetadataRowSmall(
                        movieDetails = movieDetails,
                        genres = genres,
                        modifier = Modifier.padding(horizontal = MobilePadding)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
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

        // Fixed elements overlay: AppBar (Back/Close and Cast Button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, end = MobilePadding, start = MobilePadding)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close/Back Button (Top Left)
            Box(
                modifier = Modifier
                    .size(40.dp) // Increased size for easier tapping
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onBackPressed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            // Cast Button (Top Right)
            CastButton(modifier = Modifier.padding(start = 8.dp))
        }

    }
}

@Composable
private fun CastButton(modifier: Modifier = Modifier) {
    // MediaRouteSelector is required for the MediaRouteButton to actively look for routes.
    val selector = remember {
        MediaRouteSelector.Builder()
            .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
            .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
            .build()
    }

    AndroidView(
        factory = { context ->
            val button = androidx.mediarouter.app.MediaRouteButton(context).apply {
                // Set the selector explicitly to initiate discovery
                routeSelector = selector
            }
            button
        },
        modifier = modifier
            .size(40.dp) // Match the size of the Close button
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
    )
}

@Composable
private fun MoviePosterCard(
    movie: Movie,
    tmdbImageProvider: TMDBImageProvider,
    progressPercent: Float,
    onPlayClick: () -> Unit, // Added onPlayClick parameter
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            TMDBPosterImage(
                tmdbId = movie.tmdbId,
                fallbackUrl = movie.posterUrl ?: movie.backdropUrl ?: movie.coverUrl,
                tmdbImageProvider = tmdbImageProvider,
                contentDescription = movie.name,
                modifier = Modifier.fillMaxSize()
            )
            // Playhead icon overlay
            val context = LocalContext.current
            val svgImageLoader = remember {
                ImageLoader.Builder(context)
                    .components {
                        add(SvgDecoder.Factory())
                    }
                    .build()
            }
            
            AsyncImage(
                model = "file:///android_asset/playhead.svg",
                contentDescription = "Play Icon",
                imageLoader = svgImageLoader, // Use custom loader
                modifier = Modifier
                    .size(80.dp) // Adjust size as needed
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)) // Semi-transparent background
                    .clickable(onClick = onPlayClick) // Make the entire 80.dp circle clickable
                    .padding(16.dp), // Padding to visually inset the icon
                colorFilter = ColorFilter.tint(Color.White)
            )
            
            // Progress Bar at the bottom (only display if progress is > 0 and < 0.9f)
            if (progressPercent > 0f && progressPercent < 0.9f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.5f)) // Dark background for the bar track
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressPercent.coerceIn(0f, 1f))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
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
private fun DownloadButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .size(40.dp), // Small size, matches WatchTrailerButton height
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, Color.White),
        shape = CircleShape
    ) {
        Icon(
            imageVector = Icons.Filled.Download,
            contentDescription = "Download movie",
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun HeartButton(
    isLiked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isLiked) Color.White else Color.Transparent,
            contentColor = if (isLiked) Color.Black else Color.White
        ),
        border = if (isLiked) null else BorderStroke(1.dp, Color.White),
        shape = CircleShape
    ) {
        Icon(
            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isLiked) "Remove from My List" else "Add to My List",
            modifier = Modifier.size(20.dp)
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