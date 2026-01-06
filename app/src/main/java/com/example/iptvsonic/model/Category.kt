package com.example.iptvsonic.model

import io.objectbox.annotation.Backlink
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne

@Entity
data class Category(
    @Id var id: Long = 0,
    var name: String = "",
    var contentType: Int = ContentType.liveTV
) {
    lateinit var playlist: ToOne<Playlist>

    @Backlink(to = "category")
    lateinit var channels: ToMany<Channel>

    @Backlink(to = "category")
    lateinit var movies: ToMany<Movie>

    @Backlink(to = "category")
    lateinit var tvSeries: ToMany<TvSeries>

    val isLiveTV: Boolean
        get() = contentType == ContentType.liveTV

    val isMovie: Boolean
        get() = contentType == ContentType.movie

    val isSeries: Boolean
        get() = contentType == ContentType.series
}

object ContentType {
    const val liveTV = 0
    const val movie = 1
    const val series = 2
}