package com.example.mp3player.data

import android.content.Context
import android.content.SharedPreferences
import java.io.File

/**
 * Helper class for managing playback preferences
 * Saves last played file path and position
 */
class PlaybackPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "playback_preferences"
        private const val KEY_LAST_FILE_PATH = "last_file_path"
        private const val KEY_LAST_POSITION = "last_position"
        private const val KEY_LAST_FOLDER_PATH = "last_folder_path"
    }
    
    /**
     * Save the last played file path and position
     */
    fun savePlaybackState(filePath: String, positionMs: Long, folderPath: String? = null) {
        prefs.edit().apply {
            putString(KEY_LAST_FILE_PATH, filePath)
            putLong(KEY_LAST_POSITION, positionMs)
            folderPath?.let { putString(KEY_LAST_FOLDER_PATH, it) }
            apply()
        }
    }
    
    /**
     * Get the last played file path
     */
    fun getLastFilePath(): String? {
        return prefs.getString(KEY_LAST_FILE_PATH, null)
    }
    
    /**
     * Get the last playback position
     */
    fun getLastPosition(): Long {
        return prefs.getLong(KEY_LAST_POSITION, 0)
    }
    
    /**
     * Get the last played folder path
     */
    fun getLastFolderPath(): String? {
        return prefs.getString(KEY_LAST_FOLDER_PATH, null)
    }
    
    /**
     * Clear saved playback state
     */
    fun clearPlaybackState() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Check if there's a saved playback state
     */
    fun hasSavedState(): Boolean {
        return getLastFilePath() != null
    }
}