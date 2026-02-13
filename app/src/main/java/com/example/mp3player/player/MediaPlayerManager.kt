package com.example.mp3player.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.mp3player.data.MediaFile
import com.example.mp3player.data.PlaybackPreferences
import java.io.File

/**
 * Singleton manager for media playback using ExoPlayer
 * Handles playlist management and auto-advancement
 */
class MediaPlayerManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val preferences = PlaybackPreferences(appContext)
    private var exoPlayer: ExoPlayer? = null

    private var playlist: List<MediaFile> = emptyList()
    private var currentIndex: Int = -1
    private var currentFolder: File? = null

    private val listeners = mutableListOf<PlaybackListener>()
    
    companion object {
        @Volatile
        private var instance: MediaPlayerManager? = null
        
        fun getInstance(context: Context): MediaPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: MediaPlayerManager(context).also { instance = it }
            }
        }
    }
    
    /**
     * Initialize or get the ExoPlayer instance
     */
    fun getPlayer(): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(appContext).build().apply {
                addListener(playerListener)
                playWhenReady = true
            }
        }
        return exoPlayer!!
    }
    
    /**
     * Load a playlist from a list of media files
     */
    fun loadPlaylist(files: List<MediaFile>, startIndex: Int = 0, folder: File? = null) {
        playlist = files
        currentFolder = folder
        currentIndex = startIndex.coerceIn(0, files.size - 1)
        
        if (files.isNotEmpty()) {
            playCurrentFile()
        }
        
        notifyPlaylistChanged()
    }
    
    /**
     * Play a specific file from the playlist
     */
    fun playFile(file: MediaFile) {
        val index = playlist.indexOfFirst { it.path == file.path }
        if (index >= 0) {
            currentIndex = index
            playCurrentFile()
        }
    }
    
    /**
     * Play the current file in the playlist
     */
    private fun playCurrentFile() {
        if (currentIndex < 0 || currentIndex >= playlist.size) return
        
        val file = playlist[currentIndex]
        val player = getPlayer()
        
        val mediaItem = MediaItem.fromUri(file.path)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        
        notifyTrackChanged()
    }
    
    /**
     * Play next file in playlist
     */
    fun playNext() {
        if (playlist.isEmpty()) return
        
        currentIndex = (currentIndex + 1) % playlist.size
        playCurrentFile()
    }
    
    /**
     * Play previous file in playlist
     */
    fun playPrevious() {
        if (playlist.isEmpty()) return
        
        currentIndex = if (currentIndex > 0) currentIndex - 1 else playlist.size - 1
        playCurrentFile()
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }
    
    /**
     * Seek to position in milliseconds
     */
    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }
    
    /**
     * Get current playback position
     */
    fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0
    
    /**
     * Save current playback state to preferences
     */
    fun saveCurrentPlaybackState() {
        val currentFile = getCurrentFile()
        if (currentFile != null) {
            val position = getCurrentPosition()
            preferences.savePlaybackState(
                currentFile.path,
                position,
                currentFolder?.absolutePath
            )
        }
    }
    
    /**
     * Load and restore last playback state
     * @return Pair of (MediaFile, position) if a valid state exists, null otherwise
     */
    fun restoreLastPlaybackState(): Pair<File, Long>? {
        val filePath = preferences.getLastFilePath() ?: return null
        val folderPath = preferences.getLastFolderPath()
        val position = preferences.getLastPosition()
        
        val file = File(filePath)
        if (!file.exists()) {
            preferences.clearPlaybackState()
            return null
        }
        
        // Restore folder if available
        folderPath?.let {
            val folder = File(it)
            if (folder.exists() && folder.isDirectory) {
                currentFolder = folder
            }
        }
        
        return Pair(file, position)
    }
    
    /**
     * Check if there's a saved playback state
     */
    fun hasSavedPlaybackState(): Boolean {
        return preferences.hasSavedState()
    }
    
    /**
     * Get total duration
     */
    fun getDuration(): Long = exoPlayer?.duration ?: 0
    
    /**
     * Check if currently playing
     */
    fun isPlaying(): Boolean = exoPlayer?.isPlaying ?: false
    
    /**
     * Get current media file
     */
    fun getCurrentFile(): MediaFile? {
        return if (currentIndex >= 0 && currentIndex < playlist.size) {
            playlist[currentIndex]
        } else null
    }
    
    /**
     * Get current playlist
     */
    fun getPlaylist(): List<MediaFile> = playlist
    
    /**
     * Get current index
     */
    fun getCurrentIndex(): Int = currentIndex
    
    /**
     * Get current folder
     */
    fun getCurrentFolder(): File? = currentFolder
    
    /**
     * Check if current file is video
     */
    fun isCurrentVideo(): Boolean = getCurrentFile()?.isVideo ?: false
    
    /**
     * Release resources
     */
    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        listeners.clear()
    }
    
    /**
     * Add playback listener
     */
    fun addListener(listener: PlaybackListener) {
        listeners.add(listener)
    }
    
    /**
     * Remove playback listener
     */
    fun removeListener(listener: PlaybackListener) {
        listeners.remove(listener)
    }
    
    private fun notifyTrackChanged() {
        listeners.forEach { it.onTrackChanged(getCurrentFile(), currentIndex) }
        // Save playback state when track changes
        saveCurrentPlaybackState()
    }
    
    private fun notifyPlaylistChanged() {
        listeners.forEach { it.onPlaylistChanged(playlist) }
    }
    
    private fun notifyPlaybackStateChanged(isPlaying: Boolean) {
        listeners.forEach { it.onPlaybackStateChanged(isPlaying) }
    }
    
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                // Auto-play next file when current one ends
                playNext()
            }
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            notifyPlaybackStateChanged(isPlaying)
        }
    }
    
    /**
     * Listener interface for playback events
     */
    interface PlaybackListener {
        fun onTrackChanged(file: MediaFile?, index: Int)
        fun onPlaylistChanged(playlist: List<MediaFile>)
        fun onPlaybackStateChanged(isPlaying: Boolean)
    }
}
