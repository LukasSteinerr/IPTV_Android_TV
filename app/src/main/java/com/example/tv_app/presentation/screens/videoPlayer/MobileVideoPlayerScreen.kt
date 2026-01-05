package com.example.tv_app.presentation.screens.videoPlayer

import android.net.Uri
import android.view.View
import android.widget.ImageButton
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.R
import com.example.tv_app.presentation.screens.videoPlayer.components.rememberPlayer

object MobileVideoPlayerScreen {
    const val MovieIdBundleKey = "movieId"
}

/**
 * [Work in progress] A composable screen for playing a video on mobile.
 *
 * @param onBackPressed The callback to invoke when the user presses the back button.
 * @param videoPlayerScreenViewModel The view model for the video player screen.
 */
@Composable
fun MobileVideoPlayerScreen(
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
            MobileVideoPlayerScreenContent(
                movie = s.movie,
                startPositionMillis = s.startPositionMillis,
                viewModel = viewModel,
                onBackPressed = onBackPressed,
                modifier = modifier
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MobileVideoPlayerScreenContent(
    movie: com.example.tv_app.model.Movie,
    startPositionMillis: Long,
    viewModel: VideoPlayerViewModel,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val window = (context as? androidx.activity.ComponentActivity)?.window
    
    var currentResizeMode by remember {
        mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT)
    }

    val toggleResizeMode: () -> Unit = remember {
        {
            currentResizeMode = if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            } else {
                AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        }
    }

    // Keep screen on and hide system UI for an immersive experience
    DisposableEffect(window) {
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.apply {
                hide(WindowInsetsCompat.Type.statusBars())
                hide(WindowInsetsCompat.Type.navigationBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.apply {
                    show(WindowInsetsCompat.Type.statusBars())
                    show(WindowInsetsCompat.Type.navigationBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                }
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    val trackSelector = remember { DefaultTrackSelector(context) }
    val exoPlayer = rememberPlayer(context, trackSelector)

    LaunchedEffect(exoPlayer, movie, startPositionMillis) {
        exoPlayer.addMediaItem(movie.intoMediaItem())
        exoPlayer.prepare()
        
        // Resume playback if position > 0
        if (startPositionMillis > 0L) {
            exoPlayer.seekTo(startPositionMillis)
        }

        exoPlayer.play()
    }

    // Launched effect for continuous position reporting
    LaunchedEffect(exoPlayer) {
        while (true) {
            // Report position and duration to the ViewModel
            if (exoPlayer.isPlaying) {
                viewModel.updateCurrentPosition(
                    position = exoPlayer.currentPosition,
                    duration = exoPlayer.duration
                )
            }
            // Update approximately once per second
            kotlinx.coroutines.delay(1000)
        }
    }

    BackHandler {
        // Explicitly update position one last time before releasing the player and navigating away
        viewModel.updateCurrentPosition(
            position = exoPlayer.currentPosition,
            duration = exoPlayer.duration
        )
        // Manually trigger save logic
        viewModel.saveCurrentProgress()
        
        exoPlayer.release()
        onBackPressed()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = true
                    resizeMode = currentResizeMode
                    setShowSubtitleButton(true)

                    // Find the view group holding the basic controls (e.g., subtitles, quality)
                    val basicControls: View? = findViewById(R.id.exo_basic_controls)
                    val subtitlesButton = findViewById<ImageButton>(R.id.exo_subtitle)

                    if (basicControls is android.widget.LinearLayout && subtitlesButton != null) {
                        val aspectRatioButton = ImageButton(context).apply {
                            id = View.generateViewId()
                            contentDescription = "Toggle aspect ratio"
                            setBackgroundResource(android.R.color.transparent)
                            setImageResource(android.R.drawable.ic_menu_zoom) // Use a more appropriate system icon
                            setOnClickListener { toggleResizeMode() }
                        }
                        
                        // Insert the aspect ratio button before the subtitles button
                        val index = basicControls.indexOfChild(subtitlesButton)
                        if (index >= 0) {
                            basicControls.addView(aspectRatioButton, index)
                        } else {
                            // If subtitles button is not found, just append
                            basicControls.addView(aspectRatioButton)
                        }
                    }

                }
            },
            update = {
                it.resizeMode = currentResizeMode
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun com.example.tv_app.model.Movie.intoMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setUri(streamUrl)
        .build()
}