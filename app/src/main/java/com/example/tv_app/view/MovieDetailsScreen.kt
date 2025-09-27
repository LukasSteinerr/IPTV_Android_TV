package com.example.tv_app.view

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import androidx.tv.material3.Border
import coil.compose.AsyncImage
import com.example.tv_app.model.Movie
import com.example.tv_app.model.Cast
import com.example.tv_app.model.MovieReviewsAndRatings
import com.example.tv_app.repository.TMDBService
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.presentation.common.MovieCard
import com.example.tv_app.presentation.utils.rememberChildPadding
import com.example.tv_app.presentation.theme.JetStreamButtonShape
import com.example.tv_app.presentation.theme.JetStreamCardShape
import com.example.tv_app.presentation.theme.JetStreamBorderWidth
import com.example.tv_app.presentation.components.TitleValueText
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieDetailsScreen(
    key: Int = 0, // Key to force recomposition
    movie: Movie,
    @Suppress("UNUSED_PARAMETER") playlistService: PlaylistService,
    onBackPressed: () -> Unit,
    onMovieSelected: (Movie) -> Unit = {},
    onPlayMovie: (Movie) -> Unit = {}
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
    // var isInMyList by remember { mutableStateOf(movie.myList == 1) } // Unused for now

    val coroutineScope = rememberCoroutineScope()
    val tmdbService = remember { TMDBService() }
    val tmdbImageProvider = remember { TMDBImageProvider.getInstance() }
    val lazyListState = rememberLazyListState()

    // Scroll to top when movie changes (similar movie selected)
    LaunchedEffect(key) {
        if (key > 0) { // Only scroll if key was changed (new movie selected)
            lazyListState.scrollToItem(0)
        }
    }

    LaunchedEffect(movie.tmdbId) {
        coroutineScope.launch {
            try {
                movie.tmdbId?.let { tmdbId ->
                    val details = tmdbService.getMovieDetails(tmdbId)
                    details?.let {
                        movieDetails = movie.copy(
                            description = details.optString("overview", movie.description ?: ""),
                            rating = details.optDouble("vote_average", 0.0).toString(),
                            duration = details.optInt("runtime", 0).let { if (it > 0) "${it} min" else null }
                        )
                        genres = tmdbService.parseGenres(details)
                    }
                    cast = tmdbService.getMovieCredits(tmdbId)
                    similarMovies = tmdbService.getSimilarMovies(tmdbId)
                    val images = tmdbService.getMovieImages(tmdbId)
                    posterUrl = images["poster"]
                    backdropUrl = images["backdrop"]
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
                modifier = Modifier
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
    val childPadding = rememberChildPadding()

    BackHandler(onBack = onBackPressed)
    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(bottom = 135.dp),
        modifier = modifier,
    ) {
        item {
            MovieDetailsHeader(
                movieDetails = movieDetails,
                genres = genres,
                backdropUrl = backdropUrl,
                onPlayMovie = onPlayMovie
            )
        }

        item {
            CastAndCrewList(
                cast = cast
            )
        }

        if (similarMovies.isNotEmpty()) {
            item {
                MoviesRow(
                    title = "Similar to ${movieDetails.name}",
                    movies = similarMovies,
                    onMovieSelected = onMovieSelected
                )
            }
        }

        if (reviewsAndRatings.isNotEmpty()) {
            item {
                MovieReviews(
                    modifier = Modifier.padding(top = childPadding.top),
                    reviewsAndRatings = reviewsAndRatings
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = childPadding.start)
                    .padding(BottomDividerPadding)
                    .fillMaxWidth()
                    .height(1.dp)
                    .alpha(0.15f)
                    .background(MaterialTheme.colorScheme.onSurface)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = childPadding.start),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val itemModifier = Modifier.width(192.dp)

                TitleValueText(
                    modifier = itemModifier,
                    title = "Year",
                    value = movieDetails.year ?: "Unknown"
                )
                TitleValueText(
                    modifier = itemModifier,
                    title = "Duration",
                    value = movieDetails.duration ?: "Unknown"
                )
                TitleValueText(
                    modifier = itemModifier,
                    title = "Rating",
                    value = movieDetails.rating?.let { "⭐ $it" } ?: "N/A"
                )
                TitleValueText(
                    modifier = itemModifier,
                    title = "Genre",
                    value = genres.firstOrNull() ?: "Unknown"
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MovieDetailsHeader(
    movieDetails: Movie,
    genres: List<String>,
    backdropUrl: String?,
    onPlayMovie: () -> Unit
) {
    val childPadding = rememberChildPadding()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val playButtonFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Request focus for the play button when the screen first appears
    LaunchedEffect(Unit) {
        playButtonFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(432.dp)
            .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        MovieImageWithGradients(
            movieDetails = movieDetails,
            backdropUrl = backdropUrl,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxWidth(0.55f)) {
            Spacer(modifier = Modifier.height(108.dp))
            Column(
                modifier = Modifier.padding(start = childPadding.start)
            ) {
                MovieLargeTitle(movieTitle = movieDetails.name)

                Column(
                    modifier = Modifier.alpha(0.75f)
                ) {
                    MovieDescription(description = movieDetails.description ?: "")
                    DotSeparatedRow(
                        modifier = Modifier.padding(top = 20.dp),
                        texts = listOfNotNull(
                            movieDetails.year,
                            movieDetails.duration,
                            movieDetails.rating?.let { "⭐ $it" }
                        )
                    )
                    DirectorScreenplayMusicRow(
                        director = genres.firstOrNull() ?: "Unknown",
                        screenplay = "TMDB",
                        music = "Various"
                    )
                }
                WatchTrailerButton(
                    modifier = Modifier
                        .focusRequester(playButtonFocusRequester)
                        .onFocusChanged {
                            if (it.isFocused) {
                                coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                            }
                        },
                    goToMoviePlayer = onPlayMovie
                )
            }
        }
    }
}

@Composable
private fun WatchTrailerButton(
    modifier: Modifier = Modifier,
    goToMoviePlayer: () -> Unit
) {
    Button(
        onClick = goToMoviePlayer,
        modifier = modifier.padding(top = 24.dp),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        shape = ButtonDefaults.shape(shape = JetStreamButtonShape)
    ) {
        Icon(
            imageVector = Icons.Outlined.PlayArrow,
            contentDescription = null
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "Play",
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun DirectorScreenplayMusicRow(
    director: String,
    screenplay: String,
    music: String
) {
    Row(modifier = Modifier.padding(top = 32.dp)) {
        TitleValueText(
            modifier = Modifier
                .padding(end = 32.dp)
                .weight(1f),
            title = "Genre",
            value = director
        )

        TitleValueText(
            modifier = Modifier
                .padding(end = 32.dp)
                .weight(1f),
            title = "Source",
            value = screenplay
        )

        TitleValueText(
            modifier = Modifier.weight(1f),
            title = "Audio",
            value = music
        )
    }
}

@Composable
private fun MovieDescription(description: String) {
    Text(
        text = description,
        style = MaterialTheme.typography.titleSmall.copy(
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal
        ),
        modifier = Modifier.padding(top = 8.dp),
        maxLines = 2
    )
}

@Composable
private fun MovieLargeTitle(movieTitle: String) {
    Text(
        text = movieTitle,
        style = MaterialTheme.typography.displayMedium.copy(
            fontWeight = FontWeight.Bold
        ),
        maxLines = 1
    )
}

@Composable
private fun MovieImageWithGradients(
    movieDetails: Movie,
    backdropUrl: String?,
    modifier: Modifier = Modifier,
    gradientColor: Color = MaterialTheme.colorScheme.surface,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(backdropUrl)
            .crossfade(true).build(),
        contentDescription = "Movie poster for ${movieDetails.name}",
        contentScale = ContentScale.Crop,
        modifier = modifier.drawWithContent {
            drawContent()
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, gradientColor),
                    startY = 600f
                )
            )
            drawRect(
                Brush.horizontalGradient(
                    colors = listOf(gradientColor, Color.Transparent),
                    endX = 1000f,
                    startX = 300f
                )
            )
            drawRect(
                Brush.linearGradient(
                    colors = listOf(gradientColor, Color.Transparent),
                    start = Offset(x = 500f, y = 500f),
                    end = Offset(x = 1000f, y = 0f)
                )
            )
        }
    )
}

@Composable
private fun CastAndCrewList(cast: List<Cast>) {
    val childPadding = rememberChildPadding()

    Column(
        modifier = Modifier.padding(top = childPadding.top),
    ) {
        Text(
            text = "Cast & Crew",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp
            ),
            modifier = Modifier.padding(start = childPadding.start)
        )
        LazyRow(
            modifier = Modifier
                .padding(top = 16.dp),
            contentPadding = PaddingValues(start = childPadding.start)
        ) {
            items(cast, key = { "${it.name}-${it.character}" }) {
                CastAndCrewItem(it, modifier = Modifier.width(144.dp))
            }
        }
    }
}

@Composable
private fun CastAndCrewItem(
    castMember: Cast,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .padding(end = 20.dp, bottom = 16.dp)
            .aspectRatio(1 / 1.8f),
        shape = CardDefaults.shape(shape = JetStreamCardShape),
        scale = CardDefaults.scale(focusedScale = 1f),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(
                    width = JetStreamBorderWidth,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                shape = JetStreamCardShape
            )
        ),
        onClick = {}
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.725f)
            ) {
                AsyncImage(
                    model = castMember.profilePath?.let { TMDBService.getPosterUrl(it) },
                    contentDescription = castMember.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .padding(horizontal = 12.dp),
                text = castMember.name,
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = castMember.character,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .alpha(0.75f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MoviesRow(
    title: String,
    movies: List<Movie>,
    onMovieSelected: (Movie) -> Unit
) {
    val childPadding = rememberChildPadding()
    
    Column(modifier = Modifier.padding(top = childPadding.top)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = childPadding.start, bottom = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(start = childPadding.start),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(movies) { movie ->
                MovieCard(
                    movie = movie,
                    tmdbImageProvider = TMDBImageProvider.getInstance(),
                    onClick = { onMovieSelected(movie) },
                    modifier = Modifier.width(150.dp),
                    showTitle = true
                )
            }
        }
    }
}

private val BottomDividerPadding = PaddingValues(vertical = 48.dp)