package com.example.tv_app.presentation.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Utility class to determine the type of device the application is running on.
 */
object DeviceType {

    /**
     * Checks if the device is an Android TV device.
     * This is typically determined by checking for the LEANBACK feature.
     */
    fun isTv(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    /**
     * Checks if the device is a mobile/phone device.
     * This is a fallback if it's not detected as a TV device.
     */
    fun isMobile(context: Context): Boolean {
        return !isTv(context)
    }
}