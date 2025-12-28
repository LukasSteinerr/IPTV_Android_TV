package com.example.tv_app.mobile_ui

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
import androidx.compose.ui.platform.LocalContext // Added for LocalContext
import com.example.tv_app.model.TvSeries
import com.example.tv_app.model.TvEpisode
import com.example.tv_app.model.Cast
import com.example.tv_app.repository.TMDBService
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.presentation.common.TvSeriesCard
import com.example.tv_app.presentation.components.TitleValueText
import com.example.tv_app.mobile_ui.DotSeparatedRow
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import com.example.tv_app.viewmodel.PlaylistViewModel
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
                            youtubeTrailer = details.optString("youtube_trailer", tvSeries.youtubeTrailer)
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
                backdropUrl = backdropUrl,
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
    backdropUrl: String?,
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
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackPressed)
    Box(modifier = modifier.background(Color.Black)) {
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // 1. Header with backdrop and play icon overlay
            item {
                TvSeriesDetailsHeader(
                    tvSeriesDetails = tvSeriesDetails,
                    backdropUrl = backdropUrl,
                    onPlayEpisode = { firstEpisodeToPlay?.let(onEpisodeSelected) }
                )
            }

            // 2. Title and Metadata
            item {
                Column(
                    modifier = Modifier.padding(horizontal = MobilePadding)
                ) {
                    TvSeriesLargeTitle(tvSeriesTitle = tvSeriesDetails.name)
                    Spacer(modifier = Modifier.height(8.dp))
                    MetadataRow(
                        tvSeriesDetails = tvSeriesDetails,
                        onMyListToggle = { /* TODO: Implement MyList toggle logic */ },
                        onDownload = { /* TODO: Implement Download logic for first episode/whole series? */ }
                    )
                }
            }
            
            // 3. Play Button (Pill shaped)
            item {
                PlayEpisodeButtonPill(
                    episode = firstEpisodeToPlay,
                    goToPlayer = { firstEpisodeToPlay?.let(onEpisodeSelected) },
                    modifier = Modifier.padding(horizontal = MobilePadding, vertical = 24.dp)
                )
            }
            
            // 4. Watch Trailer Button (if available)
            if (!tvSeriesDetails.youtubeTrailer.isNullOrBlank()) {
                item {
                    WatchTrailerButtonPill(
                        trailerUrl = tvSeriesDetails.youtubeTrailer!!,
                        modifier = Modifier
                            .padding(horizontal = MobilePadding)
                            .padding(bottom = 24.dp)
                    )
                }
            }

            // 5. Overview/Synopsis
            item {
                TvSeriesOverview(
                    description = tvSeriesDetails.description ?: "No description available.",
                    modifier = Modifier.padding(horizontal = MobilePadding)
                )
            }

            // 6. Season Selector
            if (availableSeasons.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = MobilePadding, vertical = 24.dp)) {
                        SeasonSelectorButton(
                            selectedSeason = selectedSeason,
                            onShowSeasonSelector = onShowSeasonSelector,
                            modifier = Modifier.height(48.dp)
                        )
                    }
                }
            }

            // 7. Episodes List (vertical scrollable list)
            if (episodes.isNotEmpty()) {
                items(episodes, key = { it.id }) { episode ->
                    EpisodeListItem(
                        episode = episode,
                        onEpisodeSelected = onEpisodeSelected,
                        onDownload = { /* TODO: Implement single episode download logic */ },
                        modifier = Modifier.padding(horizontal = MobilePadding, vertical = 8.dp)
                    )
                }
            }


            // 8. Cast and Crew List
            item {
                CastAndCrewList(
                    cast = cast,
                    modifier = Modifier.padding(top = 24.dp, bottom = 24.dp)
                )
            }

            // 9. Similar TV Series
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

            // 10. Footer details (simplified to the Flutter version's metadata footer)
            item {
                Column(
                    modifier = Modifier.padding(horizontal = MobilePadding)
                ) {
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
                            value = tvSeriesDetails.year ?: "Unknown"
                        )
                        TitleValueText(
                            title = "Rating",
                            value = tvSeriesDetails.rating?.let { "⭐ $it" } ?: "N/A"
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
                .padding(top = 24.dp, end = MobilePadding)
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
private fun TvSeriesDetailsHeader(
    tvSeriesDetails: TvSeries,
    backdropUrl: String?,
    onPlayEpisode: () -> Unit
) {
    val headerHeight = 250.dp // Reduced height for mobile look

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
    ) {
        TvSeriesImageWithGradients(
            tvSeriesDetails = tvSeriesDetails,
            backdropUrl = backdropUrl,
            modifier = Modifier.fillMaxSize()
        )

        // Play button in the center of the backdrop (Flutter style)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onPlayEpisode),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play Episode",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(60.dp)
            )
        }
    }
}

@Composable
private fun TvSeriesImageWithGradients(
    tvSeriesDetails: TvSeries,
    backdropUrl: String?,
    modifier: Modifier = Modifier,
    gradientColor: Color = Color.Black.copy(alpha = 0.7f), // Dark gradient for Netflix feel
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(backdropUrl ?: tvSeriesDetails.coverUrl)
            .crossfade(true).build(),
        contentDescription = "TV Series poster for ${tvSeriesDetails.name}",
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
private fun TvSeriesLargeTitle(tvSeriesTitle: String) {
    Text(
        text = tvSeriesTitle.uppercase(),
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
    tvSeriesDetails: TvSeries,
    onMyListToggle: () -> Unit,
    onDownload: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Year
        tvSeriesDetails.year?.let { year ->
            Text(
                text = year,
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray
            )
        }

        // Rating
        tvSeriesDetails.rating?.let { rating ->
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
                imageVector = if (tvSeriesDetails.myList == 1) Icons.Filled.Check else Icons.Filled.Add,
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
private fun PlayEpisodeButtonPill(
    episode: TvEpisode?,
    modifier: Modifier = Modifier,
    goToPlayer: () -> Unit
) {
    val buttonText = if (episode != null) {
        "PLAY S${episode.seasonNumber} E${episode.episodeNumber}"
    } else {
        "PLAY"
    }
    
    Button(
        onClick = goToPlayer,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.LightGray.copy(alpha = 0.3f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(50), // Pill shape
        enabled = episode != null
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = buttonText,
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
private fun TvSeriesOverview(
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
private fun SeasonSelectorButton(
    modifier: Modifier = Modifier,
    selectedSeason: Int,
    onShowSeasonSelector: () -> Unit
) {
    Button(
        onClick = onShowSeasonSelector,
        modifier = modifier.width(180.dp), // Fixed width for visibility
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.DarkGray.copy(alpha = 0.5f),
            contentColor = Color.White
        )
    ) {
        Text(
            text = "Season $selectedSeason",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.size(4.dp))
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = "Select Season",
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SeasonSelectorDialog(
    availableSeasons: List<Int>,
    selectedSeason: Int,
    onSeasonSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Season",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableSeasons) { season ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSeasonSelected(season)
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = season == selectedSeason,
                            onClick = { onSeasonSelected(season) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Season $season",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EpisodeListItem(
    episode: TvEpisode,
    onEpisodeSelected: (TvEpisode) -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEpisodeSelected(episode) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Episode number (Flutter style used only card/image, adding number for clarity)
        Text(
            text = "${episode.episodeNumber}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.align(Alignment.CenterVertically).width(24.dp)
        )

        // Episode thumbnail with play icon
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(68.dp) // Approx 16:9 ratio
                .clip(RoundedCornerShape(4.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(episode.coverUrl).crossfade(true).build(),
                contentDescription = "Episode ${episode.episodeNumber} thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Play button overlay
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play Episode",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Episode details
        Column(
            modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
        ) {
            Text(
                text = episode.name.ifEmpty { episode.title },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            episode.duration?.let { duration ->
                Text(
                    text = duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        // Download icon
        IconButton(
            onClick = onDownload,
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = Icons.Filled.CloudDownload,
                contentDescription = "Download Episode",
                tint = Color.White
            )
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
