package com.example.iptvsonic.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToOne

@Entity
data class TvEpisode(
    @Id var id: Long = 0,
    var title: String = "",
    var name: String = "",
    var streamUrl: String = "",
    var seasonNumber: Int = 0,
    var episodeNumber: Int = 0,
    var coverUrl: String? = null,
    var description: String? = null,
    var duration: String? = null,
    var streamId: String? = null
) {
    lateinit var series: ToOne<TvSeries>
}