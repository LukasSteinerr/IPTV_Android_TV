package com.example.tv_app.model

import android.content.Context
import io.objectbox.BoxStore

object ObjectBox {

    lateinit var boxStore: BoxStore
        private set

    fun init(context: Context) {
        boxStore = com.example.tv_app.model.MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()
    }
}