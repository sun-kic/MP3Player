package com.example.mp3player.data

import java.io.File

/**
 * Represents a media file (audio or video)
 */
data class MediaFile(
    val file: File,
    val name: String,
    val path: String,
    val extension: String,
    val isVideo: Boolean,
    val size: Long,
    val duration: Long = 0 // Duration in milliseconds, 0 if unknown
) {
    companion object {
        fun fromFile(file: File): MediaFile? {
            if (!file.isFile) return null
            
            val extension = SupportedFormats.getFileExtension(file.name)
            if (!SupportedFormats.isSupportedFile(extension)) return null
            
            return MediaFile(
                file = file,
                name = file.nameWithoutExtension,
                path = file.absolutePath,
                extension = extension,
                isVideo = SupportedFormats.isVideoFile(extension),
                size = file.length()
            )
        }
    }
    
    val displayName: String
        get() = file.name
    
    val formattedSize: String
        get() {
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1 -> String.format("%.1f GB", gb)
                mb >= 1 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }
}
