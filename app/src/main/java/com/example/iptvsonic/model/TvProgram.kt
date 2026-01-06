package com.example.iptvsonic.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import java.util.Date

@Entity
data class TvProgram(
    @Id var id: Long = 0,
    @Index var channelXmlTvId: String = "",
    var title: String = "",
    var description: String? = null,
    var startTime: Date? = null,
    var stopTime: Date? = null
)