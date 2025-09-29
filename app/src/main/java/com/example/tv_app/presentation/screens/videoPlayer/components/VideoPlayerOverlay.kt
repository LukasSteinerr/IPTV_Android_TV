package com.example.tv_app.presentation.screens.videoPlayer.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme

@Composable
fun VideoPlayerOverlay(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    isPlaying: Boolean,
    isControlsVisible: Boolean,
    centerButton: @Composable () -> Unit,
    subtitles: @Composable () -> Unit,
    showControls: () -> Unit,
    controls: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = isControlsVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        ) {
            // Center button (play/pause)
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                centerButton()
            }
            
            // Subtitles
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 200.dp)
            ) {
                subtitles()
            }
            
            // Controls at bottom
            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                controls()
            }
        }
    }
}