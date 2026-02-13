package com.example.mp3player.ui.player

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.example.mp3player.MainActivity
import com.example.mp3player.R
import com.example.mp3player.data.MediaFile
import com.example.mp3player.databinding.FragmentMediaPlayerBinding
import com.example.mp3player.player.MediaPlayerManager

/**
 * Fragment for media playback with controls
 */
class MediaPlayerFragment : Fragment(), MediaPlayerManager.PlaybackListener {

    private var _binding: FragmentMediaPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var playerManager: MediaPlayerManager
    
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 1000)
        }
    }
    
    // Auto-hide controls after 3 seconds
    private val hideControlsRunnable = Runnable {
        if (playerManager.isCurrentVideo() && playerManager.isPlaying()) {
            hideControls()
        }
    }
    
    private var controlsVisible = true
    private val HIDE_DELAY_MS = 3000L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        playerManager = MediaPlayerManager.getInstance(requireContext())
        
        setupPlayerView()
        setupControls()
        setupSeekBar()
        setupTapToShowControls()
        
        playerManager.addListener(this)
        
        // Update UI with current state
        updateNowPlaying()
        updatePlayPauseButton()
    }

    private fun setupPlayerView() {
        // Attach ExoPlayer to PlayerView
        binding.playerView.player = playerManager.getPlayer()
    }

    private fun setupControls() {
        binding.btnPlayPause.setOnClickListener {
            playerManager.togglePlayPause()
            resetHideTimer()
        }
        
        binding.btnPrevious.setOnClickListener {
            playerManager.playPrevious()
            resetHideTimer()
        }
        
        binding.btnNext.setOnClickListener {
            playerManager.playNext()
            resetHideTimer()
        }
        
        // Also reset timer when touching the controls container
        binding.controlsContainer.setOnClickListener {
            resetHideTimer()
        }
    }

    private fun setupSeekBar() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    playerManager.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Stop auto-hide while seeking
                handler.removeCallbacks(hideControlsRunnable)
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Restart auto-hide timer after seeking
                resetHideTimer()
            }
        })
    }
    
    private fun setupTapToShowControls() {
        // Tap on video player view to toggle controls
        binding.playerView.setOnClickListener {
            if (playerManager.isCurrentVideo()) {
                if (controlsVisible) {
                    hideControls()
                } else {
                    showControls()
                    // No auto-hide after manual tap - user taps again to hide
                    // This gives unlimited time to use navigation (Browse Files, etc.)
                }
            }
        }
        
        // Also handle tap on root view for video mode
        binding.root.setOnClickListener {
            if (playerManager.isCurrentVideo() && !controlsVisible) {
                showControls()
                // No auto-hide - user taps again to hide
            }
        }
    }
    
    private fun showControls() {
        binding.controlsContainer.visibility = View.VISIBLE
        binding.controlsContainer.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
        controlsVisible = true
        
        // Exit fullscreen to show ActionBar and bottom nav when showing controls
        (activity as? MainActivity)?.exitFullscreen()
    }
    
    private fun hideControls() {
        binding.controlsContainer.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                if (_binding != null) {
                    binding.controlsContainer.visibility = View.GONE
                }
            }
            .start()
        controlsVisible = false
        
        // Enter fullscreen for video to hide all UI elements
        if (playerManager.isCurrentVideo()) {
            (activity as? MainActivity)?.enterFullscreen()
        }
    }
    
    private fun resetHideTimer() {
        handler.removeCallbacks(hideControlsRunnable)
        if (playerManager.isCurrentVideo() && playerManager.isPlaying()) {
            handler.postDelayed(hideControlsRunnable, HIDE_DELAY_MS)
        }
    }

    private fun updateNowPlaying() {
        val currentFile = playerManager.getCurrentFile()
        val isVideo = currentFile?.isVideo ?: false
        
        // For video, immediately enter fullscreen BEFORE updating UI to prevent white bar
        if (isVideo) {
            // Immediately enter fullscreen for video to remove status bar and navigation
            (activity as? MainActivity)?.enterFullscreen()
            // Hide controls immediately for video
            hideControls()
            if (playerManager.isPlaying()) {
                resetHideTimer()
            }
        } else if (!isVideo) {
            // Always show controls for audio
            showControls()
            handler.removeCallbacks(hideControlsRunnable)
        }
        
        // Show/hide video player vs audio visualization
        binding.playerView.visibility = if (isVideo) View.VISIBLE else View.GONE
        binding.audioContainer.visibility = if (isVideo) View.GONE else View.VISIBLE
        
        // Update file name
        binding.tvNowPlaying.text = currentFile?.displayName ?: ""
        binding.tvAudioTitle.text = currentFile?.name ?: ""
        
        // Update track info
        val playlist = playerManager.getPlaylist()
        val index = playerManager.getCurrentIndex()
        if (playlist.isNotEmpty() && index >= 0) {
            binding.tvTrackInfo.text = getString(R.string.track_info, index + 1, playlist.size)
        } else {
            binding.tvTrackInfo.text = ""
        }
    }

    private fun updateProgress() {
        val currentPosition = playerManager.getCurrentPosition()
        val duration = playerManager.getDuration()
        
        binding.seekBar.max = duration.toInt()
        binding.seekBar.progress = currentPosition.toInt()
        
        binding.tvCurrentTime.text = formatTime(currentPosition)
        binding.tvTotalTime.text = formatTime(duration)
    }

    private fun updatePlayPauseButton() {
        val iconRes = if (playerManager.isPlaying()) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        binding.btnPlayPause.setImageResource(iconRes)
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0) return "0:00"
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateProgressRunnable)
        
        // For video, immediately enter fullscreen and start auto-hide if playing
        if (playerManager.isCurrentVideo()) {
            (activity as? MainActivity)?.enterFullscreen()
            if (playerManager.isPlaying()) {
                resetHideTimer()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateProgressRunnable)
        handler.removeCallbacks(hideControlsRunnable)
        // Save playback state when leaving the player
        playerManager.saveCurrentPlaybackState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(hideControlsRunnable)
        playerManager.removeListener(this)
        binding.playerView.player = null
        
        // Exit fullscreen when leaving player
        (activity as? MainActivity)?.exitFullscreen()
        
        _binding = null
    }

    // PlaybackListener implementation
    override fun onTrackChanged(file: MediaFile?, index: Int) {
        updateNowPlaying()
    }

    override fun onPlaylistChanged(playlist: List<MediaFile>) {
        updateNowPlaying()
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        updatePlayPauseButton()
        
        // Handle auto-hide based on play state
        if (playerManager.isCurrentVideo()) {
            if (isPlaying) {
                resetHideTimer()
            } else {
                // Show controls when paused
                handler.removeCallbacks(hideControlsRunnable)
                showControls()
            }
        }
    }
}
