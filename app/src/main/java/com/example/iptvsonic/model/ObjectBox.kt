package com.example.iptvsonic.model

import android.content.Context
import io.objectbox.BoxStore
import com.example.iptvsonic.BuildConfig
import android.util.Log

object ObjectBox {

    lateinit var boxStore: BoxStore
        private set

    fun init(context: Context) {
        boxStore = com.example.iptvsonic.model.MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()
        
        // Use ObjectBox Admin if a debug build and available (via debugImplementation)
        if (BuildConfig.DEBUG) {
            val started = io.objectbox.android.Admin(boxStore).start(context.applicationContext)
            Log.i("ObjectBoxAdmin", "Started: $started")
        }
    }
}