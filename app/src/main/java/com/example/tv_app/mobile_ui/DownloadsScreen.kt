package com.example.tv_app.mobile_ui

import androidx.compose.foundation.background
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

@Composable
fun DownloadsScreen(
    downloadRepository: DownloadRepository,
    onPlayMovie: (DownloadedMovie) -> Unit = {}
) {
    val downloads by downloadRepository.getAllDownloadsFlow().collectAsState(initial = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Dark background
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
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
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Backdrop Image
            AsyncImage(
                model = download.backdropUrl?.let { TMDBService.getBackdropUrl(it) } ?: download.posterUrl?.let { TMDBService.getPosterUrl(it) },
                contentDescription = download.movieName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Info & Progress
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = download.movieName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                when (download.status) {
                    DownloadedMovie.STATUS_DOWNLOADING, DownloadedMovie.STATUS_PAUSED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                             LinearProgressIndicator(
                                progress = { download.progress / 100f },
                                modifier = Modifier.weight(1f).height(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.Gray.copy(alpha = 0.3f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${download.progress}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                         Spacer(modifier = Modifier.height(4.dp))
                         Text(
                             text = if(download.status == DownloadedMovie.STATUS_PAUSED) "Paused" else "Downloading...",
                             style = MaterialTheme.typography.labelSmall,
                             color = if(download.status == DownloadedMovie.STATUS_PAUSED) Color.Yellow else Color.Green
                         )
                    }
                    DownloadedMovie.STATUS_COMPLETED -> {
                         Text(
                             text = "Completed",
                             style = MaterialTheme.typography.labelSmall,
                             color = Color.Green
                         )
                    }
                    DownloadedMovie.STATUS_FAILED -> {
                        Text(
                            text = "Failed",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Red
                        )
                    }
                     else -> {
                        Text(
                            text = "Pending",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (download.status == DownloadedMovie.STATUS_COMPLETED) {
                    IconButton(onClick = onPlay) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White
                        )
                    }
                } else if (download.status == DownloadedMovie.STATUS_DOWNLOADING) {
                    IconButton(onClick = onPause) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = "Pause",
                            tint = Color.White
                        )
                    }
                } else if (download.status == DownloadedMovie.STATUS_PAUSED || download.status == DownloadedMovie.STATUS_FAILED) {
                     IconButton(onClick = onResume) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Resume",
                            tint = Color.White
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
