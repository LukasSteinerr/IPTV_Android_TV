package com.example.tv_app.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class EpgChannelInfo(
    @Id var id: Long = 0,
    var channelId: String = "",
    var displayName: String = "",
    var iconUrl: String? = null
)