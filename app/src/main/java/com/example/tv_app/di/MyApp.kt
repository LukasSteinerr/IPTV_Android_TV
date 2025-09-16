package com.example.tv_app.di

import android.app.Application
import android.util.Log
import com.example.tv_app.BuildConfig
import com.example.tv_app.model.ObjectBox
import io.objectbox.android.Admin

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // ObjectBox.init() is done in App class
        ObjectBox.init(this)

        // Start ObjectBox Admin in debug builds
        if (BuildConfig.DEBUG) {
            val started = Admin(ObjectBox.boxStore).start(this)
            Log.i("ObjectBoxAdmin", "Started: $started")
        }
    }
}