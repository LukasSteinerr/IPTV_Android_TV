package com.example.tv_app.mobile_ui

import android.app.DownloadManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.tv_app.model.DownloadedMovie
import com.example.tv_app.repository.DownloadRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DownloadsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val downloadRepository = remember { DownloadRepository(context) }
    var downloads by remember { mutableStateOf(emptyList<DownloadedMovie>()) }
    var downloadStates by remember { mutableStateOf(mapOf<Long, Pair<Int, Int>>()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while(true) {
            val currentDownloads = downloadRepository.getAllDownloads()
            downloads = currentDownloads
            
            val states = mutableMapOf<Long, Pair<Int, Int>>()
            currentDownloads.forEach { movie ->
                 val status = downloadRepository.getDownloadStatus(movie.downloadId)
                 val progress = downloadRepository.updateDownloadAuth(movie.downloadId)
                 states[movie.downloadId] = status to progress
                 
                 // If status is successful, ensure progress is 100
                 if (status == DownloadManager.STATUS_SUCCESSFUL) {
                     states[movie.downloadId] = status to 100
                 }
            }
            downloadStates = states
            delay(1000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Downloads",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier
                    .padding(16.dp)
                    .padding(top = 16.dp)
            )

            if (downloads.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No downloads yet",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(downloads, key = { it.id }) { movie ->
                        val state = downloadStates[movie.downloadId] ?: (DownloadManager.STATUS_PENDING to 0)
                        DownloadItem(
                            movie = movie,
                            status = state.first,
                            progress = state.second,
                            onDelete = {
                                downloadRepository.removeDownload(movie.downloadId)
                                // Force refresh immediately provided by the loop next tick, 
                                // but we can also manually trigger or wait.
                                // For better UX, remove from list immediately?
                                // The loop updates `downloads` every second, so it might lag a bit.
                                // Let's just update local list for instant feedback
                                downloads = downloads.filter { it.downloadId != movie.downloadId }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadItem(
    movie: DownloadedMovie,
    status: Int,
    progress: Int,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Backdrop Image (Left)
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxSize()
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(movie.backdropUrl ?: movie.posterUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Overlay for better text visibility if needed, or play button if ready
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Content (Right)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = movie.movieName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                when (status) {
                    DownloadManager.STATUS_RUNNING -> {
                         Row(
                             verticalAlignment = Alignment.CenterVertically,
                             horizontalArrangement = Arrangement.spacedBy(8.dp)
                         ) {
                             LinearProgressIndicator(
                                 progress = { progress / 100f },
                                 modifier = Modifier.weight(1f).height(4.dp),
                                 color = MaterialTheme.colorScheme.primary,
                                 trackColor = Color.Gray.copy(alpha = 0.5f),
                             )
                             Text(
                                 text = "$progress%",
                                 style = MaterialTheme.typography.labelSmall,
                                 color = Color.LightGray
                             )
                         }
                    }
                    DownloadManager.STATUS_PENDING -> {
                        Text(
                            text = "Waiting...",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        Text(
                            text = "Downloaded",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Green
                        )
                    }
                    DownloadManager.STATUS_FAILED -> {
                        Text(
                            text = "Failed",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Red
                        )
                    }
                    else -> {
                         Text(
                            text = "Paused",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Delete Button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Gray
                )
            }
        }
    }
}
