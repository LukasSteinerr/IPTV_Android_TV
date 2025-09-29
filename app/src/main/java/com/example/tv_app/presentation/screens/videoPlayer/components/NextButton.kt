package com.example.tv_app.presentation.screens.videoPlayer.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.runtime.Composable
import androidx.media3.common.Player

@Composable
fun NextButton(
    player: Player,
    onShowControls: () -> Unit
) {
    VideoPlayerControlsIcon(
        icon = Icons.Default.SkipNext,
        isPlaying = player.isPlaying,
        contentDescription = "Next",
        onShowControls = onShowControls,
        onClick = {
            // Seek forward by 10 seconds
            player.seekForward()
        }
    )
}