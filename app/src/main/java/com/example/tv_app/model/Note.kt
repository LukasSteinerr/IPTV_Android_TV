package com.example.tv_app.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class Note(
    @Id var id: Long = 0,
    var text: String? = null,
    var date: Long = 0
)