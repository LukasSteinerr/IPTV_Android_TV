package com.example.tv_app.presentation.screens.videoPlayer.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player

@Composable
fun RepeatButton(
    player: Player,
    onShowControls: () -> Unit
) {
    var isRepeatMode by remember { mutableStateOf(false) }
    
    VideoPlayerControlsIcon(
        icon = Icons.Default.Replay,
        isPlaying = player.isPlaying,
        contentDescription = if (isRepeatMode) "Repeat On" else "Repeat Off",
        onShowControls = onShowControls,
        onClick = {
            isRepeatMode = !isRepeatMode
            player.repeatMode = if (isRepeatMode) {
                Player.REPEAT_MODE_ONE
            } else {
                Player.REPEAT_MODE_OFF
            }
        }
    )
}