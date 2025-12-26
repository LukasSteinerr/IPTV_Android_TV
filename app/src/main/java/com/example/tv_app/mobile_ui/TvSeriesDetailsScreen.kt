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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import coil.compose.AsyncImage
import com.example.tv_app.model.TvSeries
import com.example.tv_app.model.TvEpisode
import com.example.tv_app.model.Cast
import com.example.tv_app.repository.TMDBService
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.presentation.common.TvSeriesCard
import com.example.tv_app.presentation.components.TitleValueText
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable

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
    // var isInMyList by remember { mutableStateOf(tvSeries.myList == 1) } // Unused for now

    val coroutineScope = rememberCoroutineScope()
    val tmdbService = remember { TMDBService() }
    val tmdbImageProvider = remember { TMDBImageProvider.getInstance() }
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
                            rating = details.optDouble("vote_average", 0.0).toString()
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
                        onProgress = { progress ->
                            // Could update a state variable to show progress if needed
                        }
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
                        onProgress = { progress ->
                            // Could update a state variable to show progress if needed
                        }
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
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val childPadding = 16.dp // Fixed padding for mobile

    BackHandler(onBack = onBackPressed)
    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(bottom = 16.dp), // Reduced bottom padding for mobile
        modifier = modifier,
    ) {
        item {
            TvSeriesDetailsHeader(
                tvSeriesDetails = tvSeriesDetails,
                backdropUrl = backdropUrl,
                episodes = episodes,
                allEpisodes = allEpisodes,
                availableSeasons = availableSeasons,
                selectedSeason = selectedSeason,
                onSeasonSelected = onSeasonSelected,
                onShowSeasonSelector = onShowSeasonSelector,
                onHideSeasonSelector = onHideSeasonSelector,
                showSeasonSelector = showSeasonSelector,
                genres = genres
            )
        }

        if (episodes.isNotEmpty()) {
            item {
                EpisodesRow(
                    episodes = episodes,
                    onEpisodeSelected = onEpisodeSelected
                )
            }
        }

        item {
            CastAndCrewList(
                cast = cast
            )
        }

        if (similarTvSeries.isNotEmpty()) {
            item {
                TvSeriesRow(
                    title = "Similar to ${tvSeriesDetails.name}",
                    tvSeriesList = similarTvSeries,
                    onTvSeriesSelected = onTvSeriesSelected
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = childPadding)
                    .padding(vertical = 48.dp) // Replaced BottomDividerPadding
                    .fillMaxWidth()
                    .height(1.dp)
                    .alpha(0.15f)
                    .background(MaterialTheme.colorScheme.onSurface)
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = childPadding, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Display details vertically for mobile
                TitleValueText(
                    title = "Year",
                    value = tvSeriesDetails.year ?: "Unknown"
                )
                TitleValueText(
                    title = "Episodes",
                    value = if (episodes.isNotEmpty()) "${episodes.size} Episodes" else "Unknown"
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

@Composable
private fun TvSeriesDetailsHeader(
    tvSeriesDetails: TvSeries,
    backdropUrl: String?,
    episodes: List<TvEpisode>,
    allEpisodes: List<TvEpisode>,
    availableSeasons: List<Int>,
    selectedSeason: Int,
    onSeasonSelected: (Int) -> Unit,
    onShowSeasonSelector: () -> Unit,
    onHideSeasonSelector: () -> Unit,
    showSeasonSelector: Boolean,
    genres: List<String>
) {
    val childPadding = 16.dp // Fixed padding for mobile
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp) // Reduced height for mobile
        ) {
            TvSeriesImageWithGradients(
                tvSeriesDetails = tvSeriesDetails,
                backdropUrl = backdropUrl,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(childPadding),
                verticalArrangement = Arrangement.Bottom
            ) {
                TvSeriesLargeTitle(tvSeriesTitle = tvSeriesDetails.name)

                Column(
                    modifier = Modifier.alpha(0.75f)
                ) {
                    TvSeriesDescription(description = tvSeriesDetails.description ?: "")
                    DotSeparatedRow(
                        modifier = Modifier.padding(top = 8.dp),
                        texts = listOfNotNull(
                            tvSeriesDetails.year,
                            if (episodes.isNotEmpty()) "${episodes.size} Episodes" else null,
                            tvSeriesDetails.rating?.let { "⭐ $it" }
                        )
                    )
                }
                
                if (allEpisodes.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        SeasonSelectorButton(
                            selectedSeason = selectedSeason,
                            onShowSeasonSelector = onShowSeasonSelector,
                            modifier = Modifier.height(48.dp)
                        )
                        
                        // Add Watch Trailer button if youtubeTrailer is available
                        if (!tvSeriesDetails.youtubeTrailer.isNullOrBlank()) {
                            WatchTrailerButton(
                                onClick = {
                                    val youtubeUrl = "https://www.youtube.com/watch?v=${tvSeriesDetails.youtubeTrailer}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.height(48.dp)
                            )
                        }
                    }
                    
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
        }
    }
    // DirectorScreenplayMusicRow is removed/simplified into the details section below
}


// DirectorScreenplayMusicRow is removed/simplified into the details section below

@Composable
private fun TvSeriesDescription(description: String) {
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
private fun TvSeriesLargeTitle(tvSeriesTitle: String) {
    Text(
        text = tvSeriesTitle,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold
        ),
        color = Color.White,
        maxLines = 1
    )
}

@Composable
private fun TvSeriesImageWithGradients(
    tvSeriesDetails: TvSeries,
    backdropUrl: String?,
    modifier: Modifier = Modifier,
    gradientColor: Color = MaterialTheme.colorScheme.surface,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(backdropUrl ?: tvSeriesDetails.coverUrl)
            .crossfade(true).build(),
        contentDescription = "TV Series poster for ${tvSeriesDetails.name}",
        contentScale = ContentScale.Crop,
        modifier = modifier.drawWithContent {
            drawContent()
            // Simplified gradient for mobile readability
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                    startY = size.height * 0.5f,
                    endY = size.height
                )
            )
        }
    )
}

@Composable
private fun CastAndCrewList(cast: List<Cast>) {
    val childPadding = 16.dp

    Column(
        modifier = Modifier.padding(top = childPadding),
    ) {
        Text(
            text = "Cast & Crew",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = childPadding)
        )
        LazyRow(
            modifier = Modifier
                .padding(top = 12.dp),
            contentPadding = PaddingValues(horizontal = childPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cast, key = { "${it.name}-${it.character}" }) {
                CastAndCrewItem(it, modifier = Modifier.width(100.dp)) // Smaller card size for mobile
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
        onClick = {},
        modifier = modifier
            .aspectRatio(1 / 1.8f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
private fun EpisodesRow(
    episodes: List<TvEpisode>,
    onEpisodeSelected: (TvEpisode) -> Unit
) {
    val childPadding = 16.dp
    
    Column(modifier = Modifier.padding(top = childPadding)) {
        Text(
            text = "Season ${episodes.firstOrNull()?.seasonNumber ?: 1} Episodes",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = childPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(episodes) { episode ->
                EpisodeCard(
                    episode = episode,
                    onClick = { onEpisodeSelected(episode) }
                )
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: TvEpisode,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
        ),
        modifier = Modifier.width(200.dp) // Smaller card size for mobile
    ) {
        Column {
            // Episode image with play button overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp) // Approx 16:9 aspect ratio for 200dp width
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(episode.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Episode ${episode.episodeNumber} thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Play button overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play Episode",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp) // Smaller icon
                    )
                }
            }
            
            Column(
                modifier = Modifier.padding(12.dp) // Reduced padding
            ) {
                Text(
                    text = "E${episode.episodeNumber}: ${episode.name.ifEmpty { episode.title }}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                episode.duration?.let { duration ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TvSeriesRow(
    title: String,
    tvSeriesList: List<TvSeries>,
    onTvSeriesSelected: (TvSeries) -> Unit
) {
    val childPadding = 16.dp
    
    Column(modifier = Modifier.padding(top = childPadding)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = childPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tvSeriesList) { tvSeries ->
                TvSeriesCard(
                    tvSeries = tvSeries,
                    tmdbImageProvider = TMDBImageProvider.getInstance(),
                    onClick = { onTvSeriesSelected(tvSeries) },
                    modifier = Modifier.width(120.dp), // Smaller card size for mobile
                    showTitle = true
                )
            }
        }
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
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            text = "Season $selectedSeason",
            style = MaterialTheme.typography.titleSmall
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
private fun WatchTrailerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            text = "Trailer",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.size(4.dp))
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Watch Trailer",
            modifier = Modifier.size(20.dp)
        )
    }
}
