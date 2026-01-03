package com.example.tv_app.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Transient
import io.objectbox.relation.ToOne

@Entity
data class Movie(
    @Id var id: Long = 0,
    var name: String = "",
    var streamUrl: String = "",
    var coverUrl: String? = null,
    var description: String? = null,
    var year: String? = null,
    var duration: String? = null,
    var rating: String? = null,
    var streamId: String? = null,
    var tmdbId: String? = null,
    var posterUrl: String? = null,
    var backdropUrl: String? = null,
    var featuredPosterUrl: String? = null,
    var trailer: String? = null,
    var added: String? = null,
    var rating_5based: Double? = null,
    var myList: Int? = null,
    var isFeatured: Boolean = false
) {
    var castList: List<String>? = null

    @Transient
    var cast: List<Cast>? = null

    lateinit var category: ToOne<Category>
    lateinit var playlist: ToOne<Playlist>
}