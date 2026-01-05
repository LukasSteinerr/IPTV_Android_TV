package com.example.tv_app.presentation.utils

import java.text.DecimalFormat

/**
 * Formats a given number of bytes per second into a human-readable speed string (e.g., "1.2 MB/s").
 */
fun formatSpeed(bytesPerSecond: Long): String {
    if (bytesPerSecond <= 0) return "0 B/s"

    val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s", "TB/s")
    var value = bytesPerSecond.toDouble()
    var unitIndex = 0

    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024.0
        unitIndex++
    }

    val df = DecimalFormat("#.##")
    return "${df.format(value)} ${units[unitIndex]}"
}

/**
 * Parses a duration string (e.g., "120 min") into milliseconds.
 * Returns 0 if parsing fails.
 */
fun parseDurationToMillis(duration: String?): Long {
    if (duration == null) return 0L
    
    val parts = duration.split(" ")
    if (parts.size >= 2) {
        val value = parts[0].toLongOrNull()
        val unit = parts[1].lowercase()
        
        return when {
            value == null -> 0L
            unit.startsWith("min") -> value * 60 * 1000L
            unit.startsWith("hour") -> value * 60 * 60 * 1000L
            // Add more parsing logic if needed (e.g., HH:MM:SS format)
            else -> 0L
        }
    }
    return 0L
}