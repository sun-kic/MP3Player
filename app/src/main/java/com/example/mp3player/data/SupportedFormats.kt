package com.example.mp3player.data

/**
 * Supported media file formats
 */
object SupportedFormats {
    // Video formats
    val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "mpg", "mpeg", "avi")
    
    // Audio formats
    val AUDIO_EXTENSIONS = setOf("mp3", "wav", "flac")
    
    // All supported formats
    val ALL_EXTENSIONS = VIDEO_EXTENSIONS + AUDIO_EXTENSIONS
    
    fun isVideoFile(extension: String): Boolean {
        return VIDEO_EXTENSIONS.contains(extension.lowercase())
    }
    
    fun isAudioFile(extension: String): Boolean {
        return AUDIO_EXTENSIONS.contains(extension.lowercase())
    }
    
    fun isSupportedFile(extension: String): Boolean {
        return ALL_EXTENSIONS.contains(extension.lowercase())
    }
    
    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }
}
