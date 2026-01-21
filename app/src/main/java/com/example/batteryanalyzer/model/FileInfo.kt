package com.example.batteryanalyzer.model

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Parcelize
data class FileInfo(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long
) : Parcelable {
    @IgnoredOnParcel
    val file: File = File(path)
    val sizeFormatted: String
        get() = formatFileSize(sizeBytes)
    
    val dateFormatted: String
        get() = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            .format(Date(lastModified))
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
