package com.example.mp3player.data

import java.io.File

/**
 * Represents a folder that may contain media files
 */
data class MediaFolder(
    val file: File,
    val name: String,
    val path: String,
    val mediaFileCount: Int = 0
) {
    companion object {
        fun fromFile(file: File): MediaFolder? {
            if (!file.isDirectory) return null
            
            val mediaCount = file.listFiles()?.count { f ->
                f.isFile && SupportedFormats.isSupportedFile(
                    SupportedFormats.getFileExtension(f.name)
                )
            } ?: 0
            
            return MediaFolder(
                file = file,
                name = file.name,
                path = file.absolutePath,
                mediaFileCount = mediaCount
            )
        }
    }
}
