package com.example.tv_app.model

import io.objectbox.annotation.Backlink
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne

@Entity
data class TvSeries(
    @Id var id: Long = 0,
    var name: String = "",
    var coverUrl: String? = null,
    var description: String? = null,
    var year: String? = null,
    var rating: String? = null,
    var seriesId: String? = null,
    var tmdbId: String? = null,
    var myList: Int? = null,
    var isFeatured: Boolean = false,
    var featuredPosterUrl: String? = null,
    var lastModified: String? = null
) {
    lateinit var category: ToOne<Category>
    lateinit var playlist: ToOne<Playlist>

    @Backlink(to = "series")
    lateinit var episodes: ToMany<TvEpisode>
}