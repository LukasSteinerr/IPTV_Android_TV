package com.example.iptvsonic.mobile_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.iptvsonic.model.DownloadedMovie
import com.example.iptvsonic.repository.DownloadRepository
import com.example.iptvsonic.repository.TMDBService
import com.example.iptvsonic.mobile_ui.DotSeparatedRow

@Composable
fun DownloadsScreen(
    downloadRepository: DownloadRepository,
    onPlayMovie: (DownloadedMovie) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val downloads by downloadRepository.getAllDownloadsFlow().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume clicks to prevent them from passing to screens below */ }
            .then(modifier)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Downloads",
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Light,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Downloading,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No downloads yet",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Light,
                            letterSpacing = 0.3.sp
                        ),
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxSize()
            ) {
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

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isCompleted) Modifier.clickable(onClick = onPlay) else Modifier)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster Image
            AsyncImage(
                model = download.posterUrl?.let { TMDBService.getPosterUrl(it) } ?: download.backdropUrl?.let { TMDBService.getBackdropUrl(it) },
                contentDescription = download.movieName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(80.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.DarkGray)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Info & Progress
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                if (download.mediaType == DownloadedMovie.TYPE_TVEPISODE) {
                    Text(
                        text = download.seriesName ?: download.movieName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "S${download.seasonNumber} E${download.episodeNumber} - ${download.movieName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Text(
                        text = download.movieName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                DotSeparatedRow(
                    modifier = Modifier.fillMaxWidth(),
                    texts = listOf(
                        if (download.mediaType == DownloadedMovie.TYPE_TVEPISODE) "TV Episode" else "Movie",
                        "1h 30min"
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                when (download.status) {
                    DownloadedMovie.STATUS_DOWNLOADING, DownloadedMovie.STATUS_PAUSED -> {
                        LinearProgressIndicator(
                            progress = { download.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.2f),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${download.progress}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                    DownloadedMovie.STATUS_COMPLETED -> {
                        Text(
                            text = "Downloaded",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    DownloadedMovie.STATUS_FAILED -> {
                        Text(
                            text = "Download Failed",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    else -> {
                        Text(
                            text = "Pending",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Action buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (download.status == DownloadedMovie.STATUS_COMPLETED) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (download.status == DownloadedMovie.STATUS_DOWNLOADING) {
                    IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = "Pause",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Cancel Download",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (download.status == DownloadedMovie.STATUS_PAUSED || download.status == DownloadedMovie.STATUS_FAILED) {
                    IconButton(onClick = onResume, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = if (download.status == DownloadedMovie.STATUS_PAUSED) "Resume" else "Retry",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        Divider(
            color = Color.White.copy(alpha = 0.1f),
            thickness = 1.dp
        )
    }
}
