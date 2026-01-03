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