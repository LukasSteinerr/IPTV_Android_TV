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
import com.example.tv_app.model.Movie
import com.example.tv_app.model.Cast
import com.example.tv_app.repository.TMDBService
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.presentation.common.MovieCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieDetailsScreen(
    movie: Movie,
    playlistService: PlaylistService,
    onBackPressed: () -> Unit,
    onMovieSelected: (Movie) -> Unit = {},
    onPlayMovie: (Movie) -> Unit = {}
) {
    var movieDetails by remember { mutableStateOf<Movie?>(null) }
    var cast by remember { mutableStateOf<List<Cast>>(emptyList()) }
    var similarMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var genres by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var posterUrl by remember { mutableStateOf<String?>(null) }
    var backdropUrl by remember { mutableStateOf<String?>(null) }
    var isInMyList by remember { mutableStateOf(movie.myList == 1) }
    
    val coroutineScope = rememberCoroutineScope()
    val tmdbService = remember { TMDBService() }
    val tmdbImageProvider = remember { TMDBImageProvider.getInstance() }

    // Load TMDB data when screen is displayed
    LaunchedEffect(movie.tmdbId) {
        coroutineScope.launch {
            try {
                movie.tmdbId?.let { tmdbId ->
                    // Get movie details
                    val details = tmdbService.getMovieDetails(tmdbId)
                    details?.let { 
                        movieDetails = movie.copy(
                            description = details.optString("overview", movie.description ?: ""),
                            rating = details.optDouble("vote_average", 0.0).toString(),
                            duration = details.optInt("runtime", 0).let { if (it > 0) "${it} min" else null }
                        )
                        genres = tmdbService.parseGenres(details)
                    }
                    
                    // Get movie credits
                    cast = tmdbService.getMovieCredits(tmdbId)
                    
                    // Get similar movies
                    similarMovies = tmdbService.getSimilarMovies(tmdbId)
                    
                    // Get images
                    val images = tmdbService.getMovieImages(tmdbId)
                    posterUrl = images["poster"]
                    backdropUrl = images["backdrop"]
                }
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                movieDetails = movie // Fallback to original movie data
            }
        }
    }

    val displayMovie = movieDetails ?: movie

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
                            model = backdropUrl ?: displayMovie.backdropUrl,
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
                        
                        // Movie info at bottom
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(48.dp)
                        ) {
                            Text(
                                text = displayMovie.name,
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
                                displayMovie.year?.let { year ->
                                    Text(
                                        text = year,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                
                                displayMovie.duration?.let { duration ->
                                    Text(
                                        text = duration,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                
                                displayMovie.rating?.let { rating ->
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
                            // Play button
                            Button(
                                onClick = { onPlayMovie(displayMovie) },
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
                            
                            // My List button
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val newValue = if (isInMyList) 0 else 1
                                            // Update in database
                                            val updatedMovie = displayMovie.copy(myList = newValue)
                                            // You would update this in your database here
                                            // playlistService.updateMovie(updatedMovie)
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
                        displayMovie.description?.let { description ->
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
                        
                        // Similar Movies
                        if (similarMovies.isNotEmpty()) {
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
                                items(similarMovies.take(10)) { similarMovie ->
                                    MovieCard(
                                        movie = similarMovie,
                                        tmdbImageProvider = tmdbImageProvider,
                                        onClick = { onMovieSelected(similarMovie) },
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
