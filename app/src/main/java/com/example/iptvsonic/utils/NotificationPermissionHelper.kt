package com.example.iptvsonic.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationPermissionHelper {
    
    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    
    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(context: Context): Boolean {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires explicit notification permission
            val result = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            android.util.Log.d("NotificationPermissionHelper", "Android 13+ permission check: $result")
            result
        } else {
            // Pre-Android 13, check if notifications are enabled
            val result = NotificationManagerCompat.from(context).areNotificationsEnabled()
            android.util.Log.d("NotificationPermissionHelper", "Pre-Android 13 notification enabled check: $result")
            result
        }
        android.util.Log.d("NotificationPermissionHelper", "Final permission result: $hasPermission")
        return hasPermission
    }
    
    /**
     * Request notification permission
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // For older versions, direct to app settings
            openNotificationSettings(activity)
        }
    }
    
    /**
     * Open app notification settings
     */
    fun openNotificationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    else -> {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            // Verify the intent can be resolved before starting
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback to general app settings
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            }
        } catch (e: Exception) {
            // Last resort - open general settings
            try {
                val generalIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(generalIntent)
            } catch (ex: Exception) {
                // Log the error but don't crash
                android.util.Log.e("NotificationPermissionHelper", "Failed to open settings: ${ex.message}")
            }
        }
    }
    
    /**
     * Check if user should be shown rationale for notification permission
     */
    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            false
        }
    }
}