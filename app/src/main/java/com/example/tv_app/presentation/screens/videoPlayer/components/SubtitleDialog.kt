package com.example.tv_app.presentation.screens.videoPlayer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tv_app.model.Track

@Composable
fun SubtitleDialog(
    subtitleTracks: List<Track>,
    onSubtitleSelected: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Subtitle") },
        text = {
            Column {
                subtitleTracks.forEach { track ->
                    TextButton(onClick = { onSubtitleSelected(track) }) {
                        Text(track.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = Modifier.padding(16.dp)
    )
}
