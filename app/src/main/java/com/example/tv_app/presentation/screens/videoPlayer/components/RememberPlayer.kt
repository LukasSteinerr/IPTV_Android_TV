package com.example.tv_app.presentation.screens.videoPlayer.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun rememberPlayer(context: Context): ExoPlayer {
    val player = remember {
        ExoPlayer.Builder(context)
            .setSeekForwardIncrementMs(10_000) // 10 seconds forward
            .setSeekBackIncrementMs(10_000) // 10 seconds back
            .setMediaSourceFactory(
                ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context))
            )
            .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
            .build()
            .apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // Ensure proper cleanup when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            player.stop()
            player.release()
        }
    }

    return player
}