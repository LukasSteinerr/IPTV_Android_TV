package com.example.tv_app.di

import android.app.Application
import com.example.tv_app.model.ObjectBox
import com.jakewharton.threetenabp.AndroidThreeTen

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize ThreeTenABP for date/time handling in EPG
        AndroidThreeTen.init(this)
        
        // ObjectBox.init() is done in App class
        ObjectBox.init(this)

    }
}