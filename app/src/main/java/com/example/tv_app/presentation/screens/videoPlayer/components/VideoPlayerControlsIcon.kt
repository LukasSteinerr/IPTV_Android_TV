package com.example.tv_app.presentation.screens.videoPlayer.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme

@Composable
fun VideoPlayerControlsIcon(
    icon: ImageVector,
    isPlaying: Boolean,
    contentDescription: String,
    onShowControls: () -> Unit,
    onClick: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    
    IconButton(
        onClick = { 
            onShowControls()
            onClick?.invoke()
        },
        modifier = Modifier
            .size(48.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.size(24.dp)
        )
    }
}