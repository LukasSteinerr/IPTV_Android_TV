package com.example.iptvsonic.presentation.screens.videoPlayer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

enum class VideoPlayerMediaTitleType {
    DEFAULT, LOADING, ERROR
}

@Composable
fun VideoPlayerMediaTitle(
    title: String,
    secondaryText: String? = null,
    tertiaryText: String? = null,
    type: VideoPlayerMediaTitleType = VideoPlayerMediaTitleType.DEFAULT
) {
    Column(
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        secondaryText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        tertiaryText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}