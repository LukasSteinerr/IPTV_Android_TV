package com.example.tv_app.mobile_ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.tv_app.model.TvSeries
import com.example.tv_app.model.TvEpisode
import com.example.tv_app.model.Cast
import com.example.tv_app.repository.TMDBService
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.presentation.common.TvSeriesCard
import com.example.tv_app.presentation.components.TitleValueText
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.SuccessResult
import com.example.tv_app.model.MoviePalette
import com.example.tv_app.presentation.utils.createVerticalBackgroundGradient
import com.example.tv_app.presentation.common.TMDBTvPosterImage
import com.example.tv_app.R
import com.example.tv_app.model.ObjectBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign

// Define constant for fixed mobile padding
private val MobilePadding = 16.dp

@Composable
fun TvSeriesDetailsScreen(
    key: Int = 0, // Key to force recomposition
    tvSeries: TvSeries,
    playlistService: PlaylistService,
    onBackPressed: () -> Unit,
    onTvSeriesSelected: (TvSeries) -> Unit = {},
    onEpisodeSelected: (TvEpisode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Get the playlist associated with this TV series
    val playlist = remember { tvSeries.playlist.target }
    // Force recomposition when key changes
    LaunchedEffect(key) {
        // This will trigger when key changes, ensuring fresh state
    }
    var tvSeriesDetails by remember { mutableStateOf<TvSeries?>(null) }
    var cast by remember { mutableStateOf<List<Cast>>(emptyList()) }
    var similarTvSeries by remember { mutableStateOf<List<TvSeries>>(emptyList()) }
    var episodes by remember { mutableStateOf<List<TvEpisode>>(emptyList()) }
    var genres by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var posterUrl by remember { mutableStateOf<String?>(null) }
    var backdropUrl by remember { mutableStateOf<String?>(null) }
    var selectedSeason by remember { mutableStateOf(0) }
    var showSeasonSelector by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val tmdbService = remember { TMDBService() }
    val lazyListState = rememberLazyListState()

    // Scroll to top when tv series changes (similar series selected)
    LaunchedEffect(key) {
        if (key > 0) { // Only scroll if key was changed (new series selected)
            lazyListState.scrollToItem(0)
        }
    }

    LaunchedEffect(tvSeries.tmdbId) {
        coroutineScope.launch {
            if (tvSeries.tmdbId == null) {
                tvSeriesDetails = tvSeries
                backdropUrl = tvSeries.coverUrl
                playlist?.let { pl ->
                    episodes = playlistService.getTvSeriesEpisodes(
                        tvSeries = tvSeries,
                        playlist = pl,
                        onProgress = {}
                    )
                } ?: run {
                    episodes = emptyList()
                }
                isLoading = false
                return@launch
            }
            try {
                tvSeries.tmdbId?.let { tmdbId ->
                    val details = tmdbService.getTvSeriesDetails(tmdbId)
                    details?.let {
                        tvSeriesDetails = tvSeries.copy(
                            description = details.optString("overview", tvSeries.description ?: ""),
                            rating = details.optDouble("vote_average", 0.0).toString(),
                            youtubeTrailer = details.optString("youtube_trailer", tvSeries.youtubeTrailer ?: "")
                        )
                        genres = tmdbService.parseGenres(details)
                        val images = tmdbService.getTvSeriesImages(tmdbId, details)
                        posterUrl = images["poster"]
                        backdropUrl = images["backdrop"]
                    }
                    cast = tmdbService.getTvSeriesCredits(tmdbId)
                    val tmdbSimilarTvSeries = tmdbService.getSimilarTvSeries(tmdbId)
                    // Cross-reference similar TV series with local playlist
                    similarTvSeries = playlistService.crossReferenceSimilarTvSeries(tmdbSimilarTvSeries)
                }
                
                // Fetch episodes with progress callback
                playlist?.let { pl ->
                    episodes = playlistService.getTvSeriesEpisodes(
                        tvSeries = tvSeries,
                        playlist = pl,
                        onProgress = { /* progress */ }
                    )
                } ?: run {
                    episodes = emptyList()
                }
                
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                tvSeriesDetails = tvSeries
                backdropUrl = tvSeries.coverUrl
                playlist?.let { pl ->
                    episodes = playlistService.getTvSeriesEpisodes(
                        tvSeries = tvSeries,
                        playlist = pl,
                        onProgress = { /* progress */ }
                    )
                } ?: run {
                    episodes = emptyList()
                }
            }
        }
    }

    // Group episodes by season
    val episodesBySeason = remember(episodes) {
        episodes.groupBy { it.seasonNumber }.toSortedMap()
    }
    
    // Get available seasons
    val availableSeasons = remember(episodesBySeason) {
        episodesBySeason.keys.toList()
    }
    
    // Update selected season if it's not available
    LaunchedEffect(availableSeasons) {
        if (availableSeasons.isNotEmpty() && !availableSeasons.contains(selectedSeason)) {
            selectedSeason = availableSeasons.first()
        }
    }
    
    // Get episodes for selected season
    val currentSeasonEpisodes = remember(selectedSeason, episodesBySeason) {
        episodesBySeason[selectedSeason] ?: emptyList()
    }

    val displayTvSeries = tvSeriesDetails ?: tvSeries
    
    // Get the first episode of the selected season to suggest as the main "Play" action
    val firstEpisodeToPlay = remember(currentSeasonEpisodes) {
        currentSeasonEpisodes.firstOrNull()
    }

    val context = LocalContext.current
    var moviePalette by remember { mutableStateOf(MoviePalette()) }
    val tmdbImageProvider = remember { TMDBImageProvider.getInstance() }
    val posterForPalette = posterUrl ?: tvSeries.coverUrl

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
                tvSeriesDetails = displayTvSeries,
                cast = cast,
                similarTvSeries = similarTvSeries,
                episodes = currentSeasonEpisodes,
                allEpisodes = episodes,
                genres = genres,
                availableSeasons = availableSeasons,
                selectedSeason = selectedSeason,
                onSeasonSelected = { season -> selectedSeason = season },
                onShowSeasonSelector = { showSeasonSelector = true },
                onHideSeasonSelector = { showSeasonSelector = false },
                showSeasonSelector = showSeasonSelector,
                onBackPressed = onBackPressed,
                onTvSeriesSelected = onTvSeriesSelected,
                onEpisodeSelected = onEpisodeSelected,
                firstEpisodeToPlay = firstEpisodeToPlay,
                lazyListState = lazyListState,
                moviePalette = moviePalette,
                tmdbImageProvider = tmdbImageProvider,
                posterUrl = posterUrl,
                modifier = modifier
                    .fillMaxSize()
                    .animateContentSize()
            )
        }
    }
}

@Composable
private fun Details(
    tvSeriesDetails: TvSeries,
    cast: List<Cast>,
    similarTvSeries: List<TvSeries>,
    episodes: List<TvEpisode>,
    allEpisodes: List<TvEpisode>,
    genres: List<String>,
    availableSeasons: List<Int>,
    selectedSeason: Int,
    onSeasonSelected: (Int) -> Unit,
    onShowSeasonSelector: () -> Unit,
    onHideSeasonSelector: () -> Unit,
    showSeasonSelector: Boolean,
    onBackPressed: () -> Unit,
    onTvSeriesSelected: (TvSeries) -> Unit,
    onEpisodeSelected: (TvEpisode) -> Unit,
    firstEpisodeToPlay: TvEpisode?,
    lazyListState: LazyListState,
    moviePalette: MoviePalette,
    tmdbImageProvider: TMDBImageProvider,
    posterUrl: String?,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackPressed)
    val coroutineScope = rememberCoroutineScope()
    var isLiked by remember(tvSeriesDetails.id) { mutableStateOf(tvSeriesDetails.myList == 1) }

    // Set a solid black background color for the entire screen.
    // The palette-based gradient will be applied internally to the header content so it scrolls away.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(
                bottom = 60.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Apply the palette gradient to the top content area so it scrolls away
                        .background(brush = createVerticalBackgroundGradient(moviePalette)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Poster Card
                    MoviePosterCard(
                        tmdbId = tvSeriesDetails.tmdbId,
                        posterUrl = posterUrl ?: tvSeriesDetails.coverUrl,
                        name = tvSeriesDetails.name,
                        tmdbImageProvider = tmdbImageProvider,
                        modifier = Modifier
                            .padding(top = 80.dp, bottom = 16.dp)
                            .width(160.dp)
                            .aspectRatio(1f / 1.5f)
                    )

                    // 2. Title
                    Text(
                        text = tvSeriesDetails.name,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = MobilePadding)
                    )
                    // Display series ID for debugging purposes
                    tvSeriesDetails.seriesId?.let { seriesId ->
                        Text(
                            text = "Series ID: $seriesId",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = MobilePadding)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MobilePadding),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!tvSeriesDetails.youtubeTrailer.isNullOrBlank()) {
                            WatchTrailerButton(
                                trailerUrl = tvSeriesDetails.youtubeTrailer!!,
                                modifier = Modifier
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        DownloadButton(
                            onClick = { /* TODO */ }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        HeartButton(
                            isLiked = isLiked,
                            onClick = {
                                isLiked = !isLiked
                                coroutineScope.launch {
                                    val box = ObjectBox.boxStore.boxFor(TvSeries::class.java)
                                    val dbSeries = box.get(tvSeriesDetails.id)
                                    val newStatus = if (isLiked) 1 else 0
                                    if (dbSeries != null) {
                                        dbSeries.myList = newStatus
                                        box.put(dbSeries)
                                    }
                                    tvSeriesDetails.myList = newStatus
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Metadata
                    MetadataRowSmall(
                        tvSeriesDetails = tvSeriesDetails,
                        genres = genres,
                        modifier = Modifier.padding(horizontal = MobilePadding)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // 5. Overview Section
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
                Text(
                    text = tvSeriesDetails.description ?: stringResource(id = R.string.no_description_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    modifier = Modifier.padding(horizontal = MobilePadding)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 6. Season Selector and Episodes
            if (availableSeasons.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MobilePadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Episodes",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        SeasonSelectorButton(
                            selectedSeason = selectedSeason,
                            onShowSeasonSelector = onShowSeasonSelector
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(episodes, key = { it.id }) { episode ->
                    EpisodeListItem(
                        episode = episode,
                        onEpisodeSelected = onEpisodeSelected,
                        onDownload = { /* TODO */ },
                        modifier = Modifier.padding(horizontal = MobilePadding, vertical = 8.dp)
                    )
                }
            }

            // 7. Cast
            if (cast.isNotEmpty()) {
                item {
                    CastAndCrewList(
                        cast = cast,
                        modifier = Modifier.padding(top = 24.dp, bottom = 24.dp)
                    )
                }
            }

            // 8. Similar TV Series
            if (similarTvSeries.isNotEmpty()) {
                item {
                    TvSeriesRow(
                        title = "More Like This",
                        tvSeriesList = similarTvSeries,
                        onTvSeriesSelected = onTvSeriesSelected,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }
        }

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

        // Watch Now Button (Fixed Bottom)
        if (firstEpisodeToPlay != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = MobilePadding, vertical = 8.dp)
            ) {
                Button(
                    onClick = { onEpisodeSelected(firstEpisodeToPlay) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "WATCH S${firstEpisodeToPlay.seasonNumber} E${firstEpisodeToPlay.episodeNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // Season Selector Dialog
        if (showSeasonSelector && availableSeasons.size > 1) {
            SeasonSelectorDialog(
                availableSeasons = availableSeasons,
                selectedSeason = selectedSeason,
                onSeasonSelected = { season ->
                    onSeasonSelected(season)
                    onHideSeasonSelector()
                },
                onDismiss = onHideSeasonSelector
            )
        }
    }
}

@Composable
private fun MoviePosterCard(
    tmdbId: String?,
    posterUrl: String?,
    name: String,
    tmdbImageProvider: TMDBImageProvider,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
    ) {
        TMDBTvPosterImage(
            tmdbId = tmdbId,
            fallbackUrl = posterUrl,
            tmdbImageProvider = tmdbImageProvider,
            contentDescription = name,
            modifier = Modifier.fillMaxSize()
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
            .width(160.dp)
            .height(40.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "Watch Trailer",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun DownloadButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, Color.White),
        shape = CircleShape
    ) {
        Icon(
            imageVector = Icons.Filled.CloudDownload,
            contentDescription = "Download",
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
    tvSeriesDetails: TvSeries,
    genres: List<String>,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        tvSeriesDetails.year?.let { year ->
            Text(
                text = year,
                style = MaterialTheme.typography.labelMedium,
                color = Color.LightGray
            )
        }

        if (tvSeriesDetails.year != null && genres.isNotEmpty()) {
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
private fun SeasonSelectorButton(
    modifier: Modifier = Modifier,
    selectedSeason: Int,
    onShowSeasonSelector: () -> Unit
) {
    Surface(
        onClick = onShowSeasonSelector,
        modifier = modifier,
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Season $selectedSeason",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeasonSelectorDialog(
    availableSeasons: List<Int>,
    selectedSeason: Int,
    onSeasonSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Select Season",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
            
            LazyColumn {
                items(availableSeasons) { season ->
                    val isSelected = season == selectedSeason
                    val backgroundColor = if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backgroundColor)
                            .clickable { onSeasonSelected(season) }
                            .padding(vertical = 16.dp, horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Season $season",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.Gray
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeListItem(
    episode: TvEpisode,
    onEpisodeSelected: (TvEpisode) -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onEpisodeSelected(episode) },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Episode thumbnail
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(episode.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${episode.episodeNumber}. ${episode.name.ifEmpty { episode.title }}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                episode.duration?.let { duration ->
                    Text(
                        text = duration,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }
            }

            IconButton(onClick = onDownload) {
                Icon(
                    imageVector = Icons.Filled.CloudDownload,
                    contentDescription = "Download",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
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
            // Add 'See All' if there are more than 10 cast members
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
    Card(
        onClick = {},
        modifier = modifier
            .aspectRatio(1 / 1.8f),
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
private fun TvSeriesRow(
    title: String,
    tvSeriesList: List<TvSeries>,
    onTvSeriesSelected: (TvSeries) -> Unit,
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
            items(tvSeriesList) { tvSeries ->
                TvSeriesCard(
                    tvSeries = tvSeries,
                    tmdbImageProvider = TMDBImageProvider.getInstance(),
                    onClick = { onTvSeriesSelected(tvSeries) },
                    modifier = Modifier.width(110.dp),
                    showTitle = true
                )
            }
        }
    }
}
