package com.example.tv_app.presentation.screens.videoPlayer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.tv_app.model.Movie
import com.example.tv_app.model.Track

@Composable
fun VideoPlayerControls(
    player: Player,
    movie: Movie,
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
                secondaryText = movie.year,
                tertiaryText = movie.description?.take(50) ?: "",
                type = VideoPlayerMediaTitleType.DEFAULT
            )
        },
        mediaActions = {
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
            VideoPlayerSeeker(
                player = player,
                focusRequester = focusRequester,
                onShowControls = onShowControls,
            )
        },
        more = null
    )
}