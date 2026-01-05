package com.example.tv_app.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
data class WatchProgress(
    @Id var id: Long = 0,
    @Index var mediaId: String = "", // Unique identifier for the movie/episode
    var positionMillis: Long = 0,
    var durationMillis: Long = 0,
    var lastWatched: Long = System.currentTimeMillis() // Timestamp for sorting
)