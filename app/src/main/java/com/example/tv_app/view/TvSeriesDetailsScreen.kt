package com.example.tv_app.view

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch

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
    var isInMyList by remember { mutableStateOf(tvSeries.myList == 1) }
    
    val coroutineScope = rememberCoroutineScope()
    val tmdbService = remember { TMDBService() }
    val tmdbImageProvider = remember { TMDBImageProvider.getInstance() }

    // Load TMDB data when screen is displayed
    LaunchedEffect(tvSeries.tmdbId) {
        coroutineScope.launch {
            try {
                tvSeries.tmdbId?.let { tmdbId ->
                    // Get TV series details
                    val details = tmdbService.getTvSeriesDetails(tmdbId)
                    details?.let { 
                        tvSeriesDetails = tvSeries.copy(
                            description = details.optString("overview", tvSeries.description ?: ""),
                            rating = details.optDouble("vote_average", 0.0).toString()
                        )
                        genres = tmdbService.parseGenres(details)
                    }
                    
                    // Get TV series credits
                    cast = tmdbService.getTvSeriesCredits(tmdbId)
                    
                    // Get similar TV series
                    similarTvSeries = tmdbService.getSimilarTvSeries(tmdbId)
                    
                    // Get images
                    val images = tmdbService.getTvSeriesImages(tmdbId)
                    posterUrl = images["poster"]
                    backdropUrl = images["backdrop"]
                }
                
                // Get episodes from local playlist data
                episodes = playlistService.getTvSeriesEpisodes(tvSeries)
                
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                tvSeriesDetails = tvSeries // Fallback to original data
                // Still try to get episodes from local data
                episodes = playlistService.getTvSeriesEpisodes(tvSeries)
            }
        }
    }

    val displayTvSeries = tvSeriesDetails ?: tvSeries

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1F2E),
                        Color(0xFF0F1419)
                    )
                )
            )
    ) {
        if (isLoading) {
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
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Hero Section with backdrop
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    ) {
                        // Backdrop image
                        AsyncImage(
                            model = backdropUrl ?: displayTvSeries.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFF0F1419).copy(alpha = 0.7f),
                                            Color(0xFF0F1419)
                                        ),
                                        startY = 0f,
                                        endY = Float.POSITIVE_INFINITY
                                    )
                                )
                        )
                        
                        // Back button
                        IconButton(
                            onClick = onBackPressed,
                            modifier = Modifier
                                .padding(16.dp)
                                .size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        // TV series info at bottom
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(48.dp)
                        ) {
                            Text(
                                text = displayTvSeries.name,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                displayTvSeries.year?.let { year ->
                                    Text(
                                        text = year,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                
                                if (episodes.isNotEmpty()) {
                                    Text(
                                        text = "${episodes.size} Episodes",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                
                                displayTvSeries.rating?.let { rating ->
                                    Text(
                                        text = "⭐ $rating",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Content section
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 48.dp)
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Action buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Play button (play first episode)
                            if (episodes.isNotEmpty()) {
                                Button(
                                    onClick = { onEpisodeSelected(episodes.first()) },
                                    colors = ButtonDefaults.colors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Play",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                            
                            // My List button
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val newValue = if (isInMyList) 0 else 1
                                            // Update in database
                                            val updatedTvSeries = displayTvSeries.copy(myList = newValue)
                                            // You would update this in your database here
                                            // playlistService.updateTvSeries(updatedTvSeries)
                                            isInMyList = !isInMyList
                                        } catch (e: Exception) {
                                            // Handle error
                                        }
                                    }
                                },
                                border = ButtonDefaults.border(
                                    border = Border(
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = Color.White
                                        )
                                    )
                                ),
                                colors = ButtonDefaults.colors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White,
                                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                    focusedContentColor = Color.White
                                ),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Icon(
                                    imageVector = if (isInMyList) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isInMyList) "In My List" else "My List",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Description
                        displayTvSeries.description?.let { description ->
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        
                        // Genres
                        if (genres.isNotEmpty()) {
                            Text(
                                text = "Genres",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 24.dp)
                            ) {
                                items(genres) { genre ->
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        colors = SurfaceDefaults.colors(
                                            containerColor = Color.White.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 8.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Episodes
                        if (episodes.isNotEmpty()) {
                            Text(
                                text = "Episodes",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(bottom = 32.dp)
                            ) {
                                items(episodes.take(10)) { episode ->
                                    EpisodeCard(
                                        episode = episode,
                                        onClick = { onEpisodeSelected(episode) }
                                    )
                                }
                            }
                        }
                        
                        // Cast
                        if (cast.isNotEmpty()) {
                            Text(
                                text = "Cast",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(bottom = 32.dp)
                            ) {
                                items(cast.take(10)) { castMember ->
                                    CastCard(castMember = castMember)
                                }
                            }
                        }
                        
                        // Similar TV Series
                        if (similarTvSeries.isNotEmpty()) {
                            Text(
                                text = "More Like This",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(similarTvSeries.take(10)) { similarSeries ->
                                    TvSeriesCard(
                                        tvSeries = similarSeries,
                                        tmdbImageProvider = tmdbImageProvider,
                                        onClick = { onTvSeriesSelected(similarSeries) },
                                        modifier = Modifier.width(150.dp),
                                        showTitle = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: TvEpisode,
    onClick: () -> Unit
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
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
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = episode.name.ifEmpty { episode.title },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            episode.duration?.let { duration ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CastCard(castMember: Cast) {
    Column(
        modifier = Modifier.width(120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = castMember.profilePath?.let { TMDBService.getPosterUrl(it) },
            contentDescription = castMember.name,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = castMember.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Text(
            text = castMember.character,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
