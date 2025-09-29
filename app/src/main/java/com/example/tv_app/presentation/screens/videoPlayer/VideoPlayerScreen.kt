package com.example.tv_app.presentation.screens.videoPlayer

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.tv_app.presentation.screens.videoPlayer.components.VideoPlayerControls
import com.example.tv_app.presentation.screens.videoPlayer.components.VideoPlayerOverlay
import com.example.tv_app.presentation.screens.videoPlayer.components.rememberPlayer
import com.example.tv_app.presentation.screens.videoPlayer.components.rememberVideoPlayerState

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    onBackPressed: () -> Unit,
    viewModel: VideoPlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exoPlayer = rememberPlayer(context)
    val videoPlayerState = rememberVideoPlayerState(hideSeconds = 4)

    // Handle back button
    BackHandler(onBack = onBackPressed)

    when (val state = uiState) {
        is VideoPlayerUiState.Loading -> {
            // Show loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // You can add a loading indicator here if needed
            }
        }
        
        is VideoPlayerUiState.Ready -> {
            // Prepare and play the media
            LaunchedEffect(exoPlayer, state.movie) {
                val mediaItem = MediaItem.Builder()
                    .setUri(state.movie.streamUrl)
                    .build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            }

            VideoPlayerContent(
                exoPlayer = exoPlayer,
                movie = state.movie,
                videoPlayerState = videoPlayerState,
                onBackPressed = onBackPressed
            )
        }
        
        is VideoPlayerUiState.Error -> {
            // Show error state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // You can add an error message here if needed
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerContent(
    exoPlayer: ExoPlayer,
    movie: com.example.tv_app.model.Movie,
    videoPlayerState: com.example.tv_app.presentation.screens.videoPlayer.components.VideoPlayerState,
    onBackPressed: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusable()
    ) {
        // Video surface - using AndroidView for ExoPlayer integration
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Video controls overlay
        VideoPlayerOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            focusRequester = focusRequester,
            isPlaying = exoPlayer.isPlaying,
            isControlsVisible = videoPlayerState.isControlsVisible,
            centerButton = {
                // You can add a play/pause button here if needed
            },
            subtitles = {
                // Subtitles can be implemented here
            },
            showControls = { videoPlayerState.showControls(exoPlayer.isPlaying) },
            controls = {
                VideoPlayerControls(
                    player = exoPlayer,
                    movie = movie,
                    focusRequester = focusRequester,
                    onShowControls = { videoPlayerState.showControls(exoPlayer.isPlaying) }
                )
            }
        )
    }

    // Clean up when the composable is disposed
    LaunchedEffect(Unit) {
        // Request focus for the controls
        focusRequester.requestFocus()
    }
}