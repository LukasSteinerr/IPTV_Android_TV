package com.example.iptvsonic.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToOne

@Entity
data class Channel(
    @Id var id: Long = 0,
    var name: String = "",
    var streamUrl: String = "",
    var logoUrl: String? = null,
    var epgId: String? = null
) {
    lateinit var category: ToOne<Category>
    lateinit var playlist: ToOne<Playlist>
}