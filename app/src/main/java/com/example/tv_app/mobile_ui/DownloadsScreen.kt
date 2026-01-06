package com.example.tv_app.mobile_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.tv_app.model.DownloadedMovie
import com.example.tv_app.repository.DownloadRepository
import com.example.tv_app.repository.TMDBService
import com.example.tv_app.mobile_ui.DotSeparatedRow

@Composable
fun DownloadsScreen(
    downloadRepository: DownloadRepository,
    onPlayMovie: (DownloadedMovie) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val downloads by downloadRepository.getAllDownloadsFlow().collectAsState(initial = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Dark background
            .then(modifier)
    ) {
        if (downloads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Downloading,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No downloads yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = 24.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                item {
                    Text(
                        text = "Downloads",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(downloads, key = { it.id }) { download ->
                    DownloadItem(
                        download = download,
                        onPause = { downloadRepository.pauseDownload(download.id) },
                        onResume = { downloadRepository.resumeDownload(download.id) },
                        onDelete = { downloadRepository.deleteDownload(download.id) },
                        onPlay = { onPlayMovie(download) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadItem(
    download: DownloadedMovie,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit
) {
    val isCompleted = download.status == DownloadedMovie.STATUS_COMPLETED

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .then(if (isCompleted) Modifier.clickable(onClick = onPlay) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster Image (changed to 2:3 aspect ratio and smaller size)
        AsyncImage(
            model = download.posterUrl?.let { TMDBService.getPosterUrl(it) } ?: download.backdropUrl?.let { TMDBService.getBackdropUrl(it) },
            contentDescription = download.movieName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(96.dp) // Adjusted height
                .aspectRatio(2f / 3f) // Typical movie poster aspect ratio
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Info & Progress
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            if (download.mediaType == DownloadedMovie.TYPE_TVEPISODE) {
                // Main Title: Series Name
                Text(
                    text = download.seriesName ?: download.movieName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Subtitle: Episode Details
                Text(
                    text = "S${download.seasonNumber} E${download.episodeNumber} - ${download.movieName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp)) // Reduced spacing before metadata/progress
            } else {
                // Main Title: Movie Name
                Text(
                    text = download.movieName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp)) // Consistent spacing before metadata/progress
            }


            // Metadata Row (Type • Duration)
            // Using placeholder for Duration as it's not in DownloadedMovie model
            DotSeparatedRow(
                modifier = Modifier.fillMaxWidth(),
                texts = listOf(
                    if (download.mediaType == DownloadedMovie.TYPE_TVEPISODE) "TV Episode" else "Movie",
                    "1h 30min" // Placeholder for Duration
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar or Status
            when (download.status) {
                DownloadedMovie.STATUS_DOWNLOADING, DownloadedMovie.STATUS_PAUSED -> {
                    // Progress Bar matching the style in the reference image
                    LinearProgressIndicator(
                        progress = { download.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color.White, // Use White for the progress fill
                        trackColor = Color.White.copy(alpha = 0.2f), // Use White for the track
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Display percentage as requested
                    Text(
                        text = "${download.progress}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
                DownloadedMovie.STATUS_COMPLETED -> {
                    Text(
                        text = "Downloaded",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Green
                    )
                }
                DownloadedMovie.STATUS_FAILED -> {
                    Text(
                        text = "Download Failed",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red
                    )
                }
                else -> {
                    // STATUS_PENDING
                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
        
        // Action area (removed explicit buttons to match minimalist look, keeping implicit action space)
        // Action area - includes Play/Pause/Resume/Retry and Delete functionality
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (download.status == DownloadedMovie.STATUS_COMPLETED) {
                // Playback is handled by row click. Only show Delete button.
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (download.status == DownloadedMovie.STATUS_DOWNLOADING) {
                // Pause and Delete
                IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Cancel Download",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (download.status == DownloadedMovie.STATUS_PAUSED || download.status == DownloadedMovie.STATUS_FAILED) {
                // Resume/Retry and Delete
                IconButton(onClick = onResume, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = if (download.status == DownloadedMovie.STATUS_PAUSED) "Resume" else "Retry",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
