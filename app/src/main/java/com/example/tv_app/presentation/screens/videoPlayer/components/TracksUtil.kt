package com.example.tv_app.presentation.screens.videoPlayer.components

import androidx.media3.common.Player
import androidx.media3.common.Tracks
import com.example.tv_app.model.Track

fun getSubtitleTracks(player: Player): List<Track> {
    val subtitleTracks = mutableListOf<Track>()
    for (trackGroup in player.currentTracks.groups) {
        if (trackGroup.type == androidx.media3.common.C.TRACK_TYPE_TEXT) {
            for (i in 0 until trackGroup.length) {
                val format = trackGroup.getTrackFormat(i)
                subtitleTracks.add(
                    Track(
                        trackId = format.id ?: "",
                        language = format.language ?: "",
                        label = format.label ?: ""
                    )
                )
            }
        }
    }
    return subtitleTracks
}
