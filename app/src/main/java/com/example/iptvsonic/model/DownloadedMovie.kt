package com.example.iptvsonic.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class DownloadedMovie(
    @Id var id: Long = 0,
    var movieId: String = "",
    var movieName: String = "",
    var streamUrl: String = "",
    var posterUrl: String? = null,
    var backdropUrl: String? = null,
    var localPath: String? = null,
    // Fields for TV Episode differentiation
    var mediaType: Int = TYPE_MOVIE, // 0: Movie, 1: TvEpisode
    var seriesName: String? = null,
    var episodeNumber: Int = 0,
    var seasonNumber: Int = 0,
    var status: Int = STATUS_PENDING, // 0: Pending, 1: Downloading, 2: Paused, 3: Completed, 4: Failed
    var progress: Int = 0,
    var downloadedBytes: Long = 0,
    var totalBytes: Long = 0
) {
    companion object {
        const val STATUS_PENDING = 0
        const val STATUS_DOWNLOADING = 1
        const val STATUS_PAUSED = 2
        const val STATUS_COMPLETED = 3
        const val STATUS_FAILED = 4
        
        const val TYPE_MOVIE = 0
        const val TYPE_TVEPISODE = 1
    }
}
