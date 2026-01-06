package com.example.tv_app.presentation.screens.videoPlayer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.tv_app.model.Movie
import com.example.tv_app.model.Track

@Composable
fun VideoPlayerControls(
    player: Player,
    movie: Movie,
    isLive: Boolean,
    subtitleTracks: List<Track>,
    onSubtitleSelected: (Track) -> Unit,
    focusRequester: FocusRequester,
    onShowControls: () -> Unit = {},
) {
    val (showSubtitleMenu, setShowSubtitleMenu) = remember { mutableStateOf(false) }
    val (currentSelectedTrack, setCurrentSelectedTrack) = remember { mutableStateOf<Track?>(null) }

    SubtitleMenu(
        subtitleTracks = subtitleTracks,
        onSubtitleSelected = {
            setCurrentSelectedTrack(it)
            onSubtitleSelected(it)
            setShowSubtitleMenu(false)
        },
        onDismiss = { setShowSubtitleMenu(false) },
        isVisible = showSubtitleMenu,
        currentSelectedTrack = currentSelectedTrack
    )

    VideoPlayerMainFrame(
        mediaTitle = {
            VideoPlayerMediaTitle(
                title = movie.name,
                secondaryText = if (isLive) "LIVE" else movie.year,
                tertiaryText = movie.description?.take(100) ?: "",
                type = VideoPlayerMediaTitleType.DEFAULT
            )
        },
        mediaActions = {
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isLive) {
                    PreviousButton(
                        player = player,
                        onShowControls = onShowControls
                    )
                    NextButton(
                        player = player,
                        onShowControls = onShowControls
                    )
                    RepeatButton(
                        player = player,
                        onShowControls = onShowControls,
                    )
                }
                
                VideoPlayerControlsIcon(
                    icon = Icons.Default.AutoAwesomeMotion,
                    contentDescription = "Playlist",
                    onClick = onShowControls
                )
                VideoPlayerControlsIcon(
                    onClick = { setShowSubtitleMenu(true) },
                    icon = Icons.Default.ClosedCaption,
                    contentDescription = "Closed Captions"
                )
                VideoPlayerControlsIcon(
                    icon = Icons.Default.Settings,
                    contentDescription = "Settings",
                    onClick = onShowControls
                )
            }
        },
        seeker = {
            if (!isLive) {
                VideoPlayerSeeker(
                    player = player,
                    focusRequester = focusRequester,
                    onShowControls = onShowControls,
                )
            } else {
                // Show a simple LIVE indicator where the seeker would be
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Surface(
                        color = Color.Red,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = "LIVE",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        )
                    }
                }
            }
        },
        more = null
    )
}