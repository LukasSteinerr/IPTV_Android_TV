package com.example.tv_app.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class DownloadedMovie(
    @Id var id: Long = 0,
    var movieId: Long = 0,
    var movieName: String = "",
    var streamUrl: String = "",
    var posterUrl: String? = null,
    var backdropUrl: String? = null,
    var downloadId: Long = -1, // ID from Android DownloadManager
    var status: Int = 0, // 0: Pending, 1: Downloading, 2: Completed, 3: Failed
    var progress: Int = 0,
    var localPath: String? = null
)
