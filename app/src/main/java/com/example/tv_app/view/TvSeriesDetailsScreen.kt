package com.example.tv_app.view

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import androidx.tv.material3.Border
import coil.compose.AsyncImage
import com.example.tv_app.model.TvSeries
import com.example.tv_app.model.TvEpisode
import com.example.tv_app.model.Cast
import com.example.tv_app.repository.TMDBService
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.presentation.common.TvSeriesCard
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSeriesDetailsScreen(
    tvSeries: TvSeries,
    playlistService: PlaylistService,
    onBackPressed: () -> Unit,
    onTvSeriesSelected: (TvSeries) -> Unit = {},
    onEpisodeSelected: (TvEpisode) -> Unit = {}
) {
    var tvSeriesDetails by remember { mutableStateOf<TvSeries?>(null) }
    var cast by remember { mutableStateOf<List<Cast>>(emptyList()) }
    var similarTvSeries by remember { mutableStateOf<List<TvSeries>>(emptyList()) }
    var episodes by remember { mutableStateOf<List<TvEpisode>>(emptyList()) }
    var genres by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var posterUrl by remember { mutableStateOf<String?>(null) }
    var backdropUrl by remember { mutableStateOf<String?>(null) }
    // var isInMyList by remember { mutableStateOf(tvSeries.myList == 1) } // Unused for now

    val coroutineScope = rememberCoroutineScope()
    val tmdbService = remember { TMDBService() }
    val tmdbImageProvider = remember { TMDBImageProvider.getInstance() }

    LaunchedEffect(tvSeries.tmdbId) {
        coroutineScope.launch {
            try {
                tvSeries.tmdbId?.let { tmdbId ->
                    val details = tmdbService.getTvSeriesDetails(tmdbId)
                    details?.let {
                        tvSeriesDetails = tvSeries.copy(
                            description = details.optString("overview", tvSeries.description ?: ""),
                            rating = details.optDouble("vote_average", 0.0).toString()
                        )
                        genres = tmdbService.parseGenres(details)
                    }
                    cast = tmdbService.getTvSeriesCredits(tmdbId)
                    similarTvSeries = tmdbService.getSimilarTvSeries(tmdbId)
                    val images = tmdbService.getTvSeriesImages(tmdbId)
                    posterUrl = images["poster"]
                    backdropUrl = images["backdrop"]
                }
                episodes = playlistService.getTvSeriesEpisodes(tvSeries)
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                tvSeriesDetails = tvSeries
                episodes = playlistService.getTvSeriesEpisodes(tvSeries)
            }
        }
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
                episodes = episodes,
                genres = genres,
                backdropUrl = backdropUrl,
                onPlayEpisode = { if (episodes.isNotEmpty()) onEpisodeSelected(episodes.first()) },
                onBackPressed = onBackPressed,
                onTvSeriesSelected = onTvSeriesSelected,
                onEpisodeSelected = onEpisodeSelected,
                modifier = Modifier
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
    genres: List<String>,
    backdropUrl: String?,
    onPlayEpisode: () -> Unit,
    onBackPressed: () -> Unit,
    onTvSeriesSelected: (TvSeries) -> Unit,
    onEpisodeSelected: (TvEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val childPadding = rememberChildPadding()

    BackHandler(onBack = onBackPressed)
    LazyColumn(
        contentPadding = PaddingValues(bottom = 135.dp),
        modifier = modifier,
    ) {
        item {
            TvSeriesDetailsHeader(
                tvSeriesDetails = tvSeriesDetails,
                backdropUrl = backdropUrl,
                episodes = episodes,
                genres = genres,
                onPlayEpisode = onPlayEpisode
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
                    value = tvSeriesDetails.year ?: "Unknown"
                )
                TitleValueText(
                    modifier = itemModifier,
                    title = "Episodes",
                    value = if (episodes.isNotEmpty()) "${episodes.size} Episodes" else "Unknown"
                )
                TitleValueText(
                    modifier = itemModifier,
                    title = "Rating",
                    value = tvSeriesDetails.rating?.let { "⭐ $it" } ?: "N/A"
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
private fun TvSeriesDetailsHeader(
    tvSeriesDetails: TvSeries,
    backdropUrl: String?,
    episodes: List<TvEpisode>,
    genres: List<String>,
    onPlayEpisode: () -> Unit
) {
    val childPadding = rememberChildPadding()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(432.dp)
            .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        TvSeriesImageWithGradients(
            tvSeriesDetails = tvSeriesDetails,
            backdropUrl = backdropUrl,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxWidth(0.55f)) {
            Spacer(modifier = Modifier.height(108.dp))
            Column(
                modifier = Modifier.padding(start = childPadding.start)
            ) {
                TvSeriesLargeTitle(tvSeriesTitle = tvSeriesDetails.name)

                Column(
                    modifier = Modifier.alpha(0.75f)
                ) {
                    TvSeriesDescription(description = tvSeriesDetails.description ?: "")
                    DotSeparatedRow(
                        modifier = Modifier.padding(top = 20.dp),
                        texts = listOfNotNull(
                            tvSeriesDetails.year,
                            if (episodes.isNotEmpty()) "${episodes.size} Episodes" else null,
                            tvSeriesDetails.rating?.let { "⭐ $it" }
                        )
                    )
                    DirectorScreenplayMusicRow(
                        director = genres.firstOrNull() ?: "Unknown",
                        screenplay = "TMDB",
                        music = "Various"
                    )
                }
                if (episodes.isNotEmpty()) {
                    WatchTrailerButton(
                        modifier = Modifier.onFocusChanged {
                            if (it.isFocused) {
                                coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                            }
                        },
                        goToTvSeriesPlayer = onPlayEpisode
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchTrailerButton(
    modifier: Modifier = Modifier,
    goToTvSeriesPlayer: () -> Unit
) {
    Button(
        onClick = goToTvSeriesPlayer,
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
        style = MaterialTheme.typography.displayMedium.copy(
            fontWeight = FontWeight.Bold
        ),
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
        model = ImageRequest.Builder(LocalContext.current).data(backdropUrl)
            .crossfade(true).build(),
        contentDescription = "TV Series poster for ${tvSeriesDetails.name}",
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
private fun EpisodesRow(
    episodes: List<TvEpisode>,
    onEpisodeSelected: (TvEpisode) -> Unit
) {
    val childPadding = rememberChildPadding()
    
    Column(modifier = Modifier.padding(top = childPadding.top)) {
        Text(
            text = "Episodes",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = childPadding.start, bottom = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(start = childPadding.start),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
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
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
        ),
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Episode ${episode.episodeNumber}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = episode.name.ifEmpty { episode.title },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
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

@Composable
private fun TvSeriesRow(
    title: String,
    tvSeriesList: List<TvSeries>,
    onTvSeriesSelected: (TvSeries) -> Unit
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
            items(tvSeriesList) { tvSeries ->
                TvSeriesCard(
                    tvSeries = tvSeries,
                    tmdbImageProvider = TMDBImageProvider.getInstance(),
                    onClick = { onTvSeriesSelected(tvSeries) },
                    modifier = Modifier.width(150.dp),
                    showTitle = true
                )
            }
        }
    }
}

private val BottomDividerPadding = PaddingValues(vertical = 48.dp)