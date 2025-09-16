package com.example.tv_app.model

import io.objectbox.annotation.Backlink
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToMany
import java.util.Date

@Entity
data class Playlist(
    @Id var id: Long = 0,
    var name: String = "",
    var url: String = "",
    var username: String? = null,
    var password: String? = null,
    var typeInt: Int = 0,
    var lastUpdated: Date = Date()
) {
    @Backlink(to = "playlist")
    lateinit var channels: ToMany<Channel>

    @Backlink(to = "playlist")
    lateinit var categories: ToMany<Category>

    val isM3u: Boolean
        get() = typeInt == PlaylistTypeConstants.m3u

    val isXtream: Boolean
        get() = typeInt == PlaylistTypeConstants.xtream

    val typeName: String
        get() = if (isM3u) "M3U" else "Xtream"
}

object PlaylistTypeConstants {
    const val m3u = 0
    const val xtream = 1
}