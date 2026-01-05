package com.example.tv_app.di

import android.app.Application
import com.example.tv_app.model.ObjectBox
import com.google.android.gms.cast.framework.CastContext
import com.jakewharton.threetenabp.AndroidThreeTen

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize ThreeTenABP for date/time handling in EPG
        AndroidThreeTen.init(this)
        
        // ObjectBox.init() is done in App class
        ObjectBox.init(this)

        // Ensure Cast Context is initialized as early as possible
        CastContext.getSharedInstance(this)
    }
}