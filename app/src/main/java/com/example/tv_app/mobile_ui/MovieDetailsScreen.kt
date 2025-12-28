package com.example.tv_app.mobile_ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.tv_app.model.Movie
import com.example.tv_app.model.Cast
import com.example.tv_app.model.MovieReviewsAndRatings
import com.example.tv_app.repository.TMDBService
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.presentation.common.MovieCard
import com.example.tv_app.presentation.components.TitleValueText
import com.example.tv_app.mobile_ui.DotSeparatedRow // Corrected import
import com.example.tv_app.mobile_ui.MovieReviews // Existing screen review component
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri

// Define constant for fixed mobile padding
private val MobilePadding = 16.dp

@Composable
fun MovieDetailsScreen(
    key: Int = 0, // Key to force recomposition
    movie: Movie,
    @Suppress("UNUSED_PARAMETER") playlistService: PlaylistService,
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
    val lazyListState = rememberLazyListState()

    // Scroll to top when movie changes (similar movie selected)
    LaunchedEffect(key) {
        if (key > 0) { // Only scroll if key was changed (new movie selected)
            lazyListState.scrollToItem(0)
        }
    }

    LaunchedEffect(movie.tmdbId) {
        coroutineScope.launch {
            if (movie.tmdbId == null) {
                movieDetails = movie
                backdropUrl = movie.backdropUrl ?: movie.posterUrl
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
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                movieDetails = movie
                backdropUrl = movie.backdropUrl ?: movie.posterUrl
            }
        }
    }

    val displayMovie = movieDetails ?: movie

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
                reviewsAndRatings = reviewsAndRatings,
                backdropUrl = backdropUrl,
                onPlayMovie = { onPlayMovie(displayMovie) },
                onBackPressed = onBackPressed,
                onMovieSelected = onMovieSelected,
                lazyListState = lazyListState,
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
    reviewsAndRatings: List<MovieReviewsAndRatings>,
    backdropUrl: String?,
    onPlayMovie: () -> Unit,
    onBackPressed: () -> Unit,
    onMovieSelected: (Movie) -> Unit,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackPressed)
    Box(modifier = modifier.background(Color.Black)) { // Ensure background is black for Netflix look
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // 1. Header with backdrop and play icon overlay
            item {
                MovieDetailsHeader(
                    movieDetails = movieDetails,
                    backdropUrl = backdropUrl,
                    onPlayMovie = onPlayMovie
                )
            }

            // 2. Title and Metadata
            item {
                Column(
                    modifier = Modifier.padding(horizontal = MobilePadding)
                ) {
                    MovieLargeTitle(movieTitle = movieDetails.name)
                    Spacer(modifier = Modifier.height(8.dp))
                    MetadataRow(
                        movieDetails = movieDetails,
                        onMyListToggle = { /* TODO: Implement MyList toggle logic */ },
                        onDownload = { /* TODO: Implement Download logic */ }
                    )
                }
            }

            // 3. Play Button (Pill shaped)
            item {
                PlayMovieButtonPill(
                    goToMoviePlayer = onPlayMovie,
                    modifier = Modifier.padding(horizontal = MobilePadding, vertical = 24.dp)
                )
            }
            
            // 4. Watch Trailer Button (if available)
            if (!movieDetails.trailer.isNullOrBlank()) {
                item {
                    WatchTrailerButtonPill(
                        trailerUrl = movieDetails.trailer!!,
                        modifier = Modifier.padding(horizontal = MobilePadding)
                    )
                }
            }

            // 5. Overview/Synopsis
            item {
                MovieOverview(
                    description = movieDetails.description ?: "No description available.",
                    modifier = Modifier.padding(horizontal = MobilePadding, vertical = 24.dp)
                )
            }

            // 6. Cast and Crew List
            item {
                CastAndCrewList(
                    cast = cast,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            // 7. Similar Movies
            if (similarMovies.isNotEmpty()) {
                item {
                    MoviesRow(
                        title = "More Like This",
                        movies = similarMovies,
                        onMovieSelected = onMovieSelected,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }

            // 8. Footer details (simplified to the Flutter version's metadata footer)
            item {
                Column(
                    modifier = Modifier.padding(horizontal = MobilePadding)
                ) {
                    // Movie Reviews (if available)
                    if (reviewsAndRatings.isNotEmpty()) {
                        MovieReviews(
                            modifier = Modifier.padding(top = 24.dp),
                            reviewsAndRatings = reviewsAndRatings
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(vertical = 24.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .alpha(0.15f)
                            .background(MaterialTheme.colorScheme.onSurface)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TitleValueText(
                            title = "Year",
                            value = movieDetails.year ?: "Unknown"
                        )
                        TitleValueText(
                            title = "Duration",
                            value = movieDetails.duration ?: "Unknown"
                        )
                        TitleValueText(
                            title = "Rating",
                            value = movieDetails.rating?.let { "⭐ $it" } ?: "N/A"
                        )
                        TitleValueText(
                            title = "Genre",
                            value = genres.firstOrNull() ?: "Unknown"
                        )
                    }
                }
            }
        }

        // Close button absolute positioning (Flutter style)
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
                imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Using back arrow as a close icon replacement
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MovieDetailsHeader(
    movieDetails: Movie,
    backdropUrl: String?,
    onPlayMovie: () -> Unit
) {
    val headerHeight = 250.dp // Reduced height for mobile look

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
    ) {
        MovieImageWithGradients(
            movieDetails = movieDetails,
            backdropUrl = backdropUrl,
            modifier = Modifier.fillMaxSize()
        )

        // Play button in the center of the backdrop (Flutter style)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onPlayMovie),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play Movie",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(60.dp)
            )
        }
    }
}

@Composable
private fun MovieImageWithGradients(
    movieDetails: Movie,
    backdropUrl: String?,
    modifier: Modifier = Modifier,
    gradientColor: Color = Color.Black.copy(alpha = 0.7f), // Dark gradient for Netflix feel
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(backdropUrl ?: movieDetails.coverUrl)
            .crossfade(true).build(),
        contentDescription = "Movie poster for ${movieDetails.name}",
        contentScale = ContentScale.Crop,
        modifier = modifier.drawWithContent {
            drawContent()
            // Gradient overlay for better text visibility (top transparent to bottom black)
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, gradientColor),
                    startY = size.height * 0.5f,
                    endY = size.height
                )
            )
        }
    )
}

@Composable
private fun MovieLargeTitle(movieTitle: String) {
    Text(
        text = movieTitle.uppercase(),
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        ),
        color = Color.White,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun MetadataRow(
    movieDetails: Movie,
    onMyListToggle: () -> Unit,
    onDownload: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Year
        movieDetails.year?.let { year ->
            Text(
                text = year,
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray
            )
        }

        // Duration
        movieDetails.duration?.let { duration ->
            Text(
                text = duration,
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray
            )
        }

        // Rating (Using a basic text representation for now)
        movieDetails.rating?.let { rating ->
            Text(
                text = "⭐ ${String.format("%.1f", rating.toDoubleOrNull() ?: 0.0)}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray
            )
        }

        // HD Tag
        Box(
            modifier = Modifier
                .border(1.dp, Color.Gray, RoundedCornerShape(2.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "HD",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // My List Toggle (Placeholder logic)
        IconButton(onClick = onMyListToggle) {
            Icon(
                imageVector = if (movieDetails.myList == 1) Icons.Filled.Check else Icons.Filled.Add,
                contentDescription = "My List",
                tint = Color.White
            )
        }

        // Download Button
        IconButton(onClick = onDownload) {
            Icon(
                imageVector = Icons.Filled.CloudDownload,
                contentDescription = "Download",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun PlayMovieButtonPill(
    modifier: Modifier = Modifier,
    goToMoviePlayer: () -> Unit
) {
    // This is the big, grey, pill-shaped play button (same style as Flutter's ElevatedButton)
    Button(
        onClick = goToMoviePlayer,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.LightGray.copy(alpha = 0.3f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(50) // Pill shape
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "PLAY",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun WatchTrailerButtonPill(
    trailerUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Button(
        onClick = {
            val youtubeUrl = "https://www.youtube.com/watch?v=$trailerUrl"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))
            context.startActivity(intent)
        },
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.LightGray.copy(alpha = 0.1f), // Slightly darker/less prominent than Play
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(50) // Pill shape
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "WATCH TRAILER",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun MovieOverview(
    description: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Overview",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray,
            maxLines = 3, // Simplified as per mobile pattern
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CastAndCrewList(cast: List<Cast>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(start = MobilePadding)) {
        Text(
            text = "Top Cast", // Changed title to match Flutter
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(end = MobilePadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cast.take(10), key = { "${it.name}-${it.character}" }) {
                CastAndCrewItem(it, modifier = Modifier.width(80.dp)) // Smaller width to match Flutter
            }
            // Add 'See All' if there are more than 10 cast members (Flutter logic)
            if (cast.size > 10) {
                item {
                    SeeAllCastButton(onClick = { /* TODO: Implement navigation to AllActorsScreen */ })
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
            .height(144.dp) // Adjusted height to match list item height
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
                imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Using back arrow as a directional icon
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
    // Using standard M3 card for mobile appearance
    Card(
        onClick = {},
        modifier = modifier
            .aspectRatio(1 / 1.8f),
        shape = RoundedCornerShape(4.dp), // Small radius
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.5f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.725f) // Using weight instead of fillMaxHeight
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
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), // Smaller text
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
                    modifier = Modifier.width(110.dp), // Smaller card width
                    showTitle = true
                )
            }
        }
    }
}