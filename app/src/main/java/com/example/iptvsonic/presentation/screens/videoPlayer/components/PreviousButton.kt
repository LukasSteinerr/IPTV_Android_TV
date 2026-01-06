package com.example.iptvsonic.presentation.screens.videoPlayer.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.media3.common.Player

@Composable
fun PreviousButton(
    player: Player,
    onShowControls: () -> Unit
) {
    VideoPlayerControlsIcon(
        icon = Icons.Default.SkipPrevious,
        contentDescription = "Previous",
        onClick = {
            // Seek backward by 10 seconds
            player.seekBack()
            onShowControls()
        }
    )
}