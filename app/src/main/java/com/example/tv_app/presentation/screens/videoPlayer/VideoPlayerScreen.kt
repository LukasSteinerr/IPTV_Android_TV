package com.example.tv_app.presentation.screens.videoPlayer

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.tv_app.presentation.screens.videoPlayer.components.VideoPlayerControls
import com.example.tv_app.presentation.screens.videoPlayer.components.VideoPlayerOverlay
import com.example.tv_app.presentation.screens.videoPlayer.components.VideoPlayerPulse
import com.example.tv_app.presentation.screens.videoPlayer.components.VideoPlayerPulseState
import com.example.tv_app.presentation.screens.videoPlayer.components.VideoPlayerPulse.Type.BACK
import com.example.tv_app.presentation.screens.videoPlayer.components.VideoPlayerPulse.Type.FORWARD
import com.example.tv_app.presentation.screens.videoPlayer.components.rememberVideoPlayerPulseState
import com.example.tv_app.presentation.screens.videoPlayer.components.rememberPlayer
import com.example.tv_app.presentation.screens.videoPlayer.components.rememberVideoPlayerState
import com.example.tv_app.presentation.utils.handleDPadKeyEvents
import com.example.tv_app.presentation.screens.videoPlayer.components.getSubtitleTracks
import com.example.tv_app.presentation.utils.DeviceType // Added import
import com.example.tv_app.presentation.screens.videoPlayer.MobileVideoPlayerScreen // Added import

object VideoPlayerScreen {
    const val MovieIdBundleKey = "movieId"
}

/**
 * [Work in progress] A composable screen for playing a video.
 *
 * @param onBackPressed The callback to invoke when the user presses the back button.
 * @param videoPlayerScreenViewModel The view model for the video player screen.
 */
@Composable
fun VideoPlayerScreen(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VideoPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    if (DeviceType.isTv(context)) {
        TvVideoPlayerScreen(onBackPressed, modifier, viewModel)
    } else {
        MobileVideoPlayerScreen(onBackPressed, modifier, viewModel)
    }
}

@Composable
fun TvVideoPlayerScreen(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VideoPlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // TODO: Handle Loading & Error states
    when (val s = uiState) {
        is VideoPlayerUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // You can add a loading indicator here if needed
            }
        }

        is VideoPlayerUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // You can add an error message here if needed
            }
        }

        is VideoPlayerUiState.Ready -> {
            TvVideoPlayerScreenContent(
                movie = s.movie,
                isLive = s.isLive,
                onBackPressed = onBackPressed,
                modifier = modifier
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun TvVideoPlayerScreenContent(
    movie: com.example.tv_app.model.Movie,
    isLive: Boolean,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val window = (context as? androidx.activity.ComponentActivity)?.window

    // Keep screen on while video is playing
    DisposableEffect(Unit) {
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val trackSelector = remember { DefaultTrackSelector(context) }
    val exoPlayer = rememberPlayer(context, trackSelector)

    val videoPlayerState = rememberVideoPlayerState(
        hideSeconds = 4,
    )

    LaunchedEffect(exoPlayer, movie) {
        exoPlayer.addMediaItem(movie.intoMediaItem())
        exoPlayer.prepare()
    }

    val pulseState = rememberVideoPlayerPulseState()

    BackHandler {
        exoPlayer.release()
        onBackPressed()
    }

    Box(
        modifier
            .background(Color.Black)
            .fillMaxSize()
            .clickable { videoPlayerState.showControls(exoPlayer.isPlaying) } // Use click for controls visibility
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        val focusRequester = remember { FocusRequester() }
        VideoPlayerOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            focusRequester = focusRequester,
            isPlaying = exoPlayer.isPlaying,
            isControlsVisible = videoPlayerState.isControlsVisible,
            centerButton = { VideoPlayerPulse(pulseState) },
            showControls = videoPlayerState::showControls,
            controls = {
                VideoPlayerControls(
                    player = exoPlayer,
                    movie = movie,
                    isLive = isLive,
                    subtitleTracks = getSubtitleTracks(exoPlayer),
                    onSubtitleSelected = { track ->
                        if (track.language == "off") {
                            // Disable subtitles
                            trackSelector.setParameters(
                                trackSelector.buildUponParameters()
                                    .setPreferredTextLanguage(null)
                            )
                        } else {
                            // Enable selected subtitle
                            trackSelector.setParameters(
                                trackSelector.buildUponParameters()
                                    .setPreferredTextLanguage(track.language)
                            )
                        }
                    },
                    focusRequester = focusRequester,
                    onShowControls = { videoPlayerState.showControls(exoPlayer.isPlaying) },
                )
            }
        )
    }
}

private fun com.example.tv_app.model.Movie.intoMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setUri(streamUrl)
        .build()
}