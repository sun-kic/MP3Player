package com.example.mp3player.ui.player

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import com.example.mp3player.MainActivity
import com.example.mp3player.R
import com.example.mp3player.data.MediaFile
import com.example.mp3player.databinding.FragmentMediaPlayerBinding
import com.example.mp3player.player.MediaPlayerManager

/**
 * Fragment for media playback with YouTube-style gesture controls
 */
class MediaPlayerFragment : Fragment(), MediaPlayerManager.PlaybackListener {

    private var _binding: FragmentMediaPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var playerManager: MediaPlayerManager
    private lateinit var gestureDetector: GestureDetectorCompat
    
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 1000)
        }
    }
    
    private val hideControlsRunnable = Runnable {
        if (playerManager.isCurrentVideo() && playerManager.isPlaying()) {
            hideControls()
        }
    }
    
    private var controlsVisible = true
    private val HIDE_DELAY_MS = 3000L
    
    // Track double tap to prevent single tap from toggling controls
    private var isDoubleTapPending = false
    private var lastTapTime = 0L
    private val DOUBLE_TAP_TIMEOUT = 300L

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
        setupGestures()
        
        playerManager.addListener(this)
        
        updateNowPlaying()
        updatePlayPauseButton()
    }

    private fun setupPlayerView() {
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

        binding.btnExitFullscreen.setOnClickListener {
            val mainActivity = activity as? MainActivity
            mainActivity?.let {
                hideControlsWithoutFullscreen()
                it.exitFullscreen()
            }
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
                handler.removeCallbacks(hideControlsRunnable)
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                resetHideTimer()
            }
        })
    }
    
    private fun setupGestures() {
        gestureDetector = GestureDetectorCompat(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                val currentTime = System.currentTimeMillis()
                val timeSinceLastTap = currentTime - lastTapTime
                isDoubleTapPending = timeSinceLastTap < DOUBLE_TAP_TIMEOUT
                return true
            }
            
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                // Only toggle controls if not part of a double tap sequence
                if (!isDoubleTapPending && playerManager.isCurrentVideo()) {
                    if (controlsVisible) {
                        hideControls()
                    } else {
                        showControls()
                    }
                }
                return true
            }
            
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (!playerManager.isCurrentVideo()) {
                    return false
                }
                
                lastTapTime = System.currentTimeMillis()
                
                // YouTube logic:
                // - Controls visible: hide them without seeking
                // - Controls hidden: perform seek
                if (controlsVisible) {
                    hideControls()
                } else {
                    val screenWidth = binding.playerView.width
                    val x = e.x
                    
                    if (x < screenWidth / 2) {
                        val currentPosition = playerManager.getCurrentPosition()
                        val newPosition = (currentPosition - 10000).coerceAtLeast(0)
                        playerManager.seekTo(newPosition)
                        showToast("后退 10 秒")
                    } else {
                        val currentPosition = playerManager.getCurrentPosition()
                        val duration = playerManager.getDuration()
                        val newPosition = (currentPosition + 10000).coerceAtMost(duration)
                        playerManager.seekTo(newPosition)
                        showToast("快进 10 秒")
                    }
                }
                return true
            }
        })
        
        binding.playerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    private fun showControls() {
        binding.controlsContainer.visibility = View.VISIBLE
        binding.controlsContainer.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
        controlsVisible = true
        
        if (playerManager.isCurrentVideo()) {
            binding.btnExitFullscreen.visibility = View.VISIBLE
        }
    }
    
    private fun hideControls() {
        hideControlsWithoutFullscreen()
        
        if (playerManager.isCurrentVideo()) {
            (activity as? MainActivity)?.enterFullscreen()
        }
    }
    
    private fun hideControlsWithoutFullscreen() {
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
        
        binding.btnExitFullscreen.visibility = View.GONE
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
        
        if (isVideo) {
            (activity as? MainActivity)?.enterFullscreen()
            hideControls()
            if (playerManager.isPlaying()) {
                resetHideTimer()
            }
        } else {
            showControls()
            handler.removeCallbacks(hideControlsRunnable)
        }
        
        binding.playerView.visibility = if (isVideo) View.VISIBLE else View.GONE
        binding.audioContainer.visibility = if (isVideo) View.GONE else View.VISIBLE
        
        if (!isVideo) {
            binding.btnExitFullscreen.visibility = View.GONE
        }
        
        binding.tvNowPlaying.text = currentFile?.displayName ?: ""
        binding.tvAudioTitle.text = currentFile?.name ?: ""
        
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
        playerManager.saveCurrentPlaybackState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(hideControlsRunnable)
        playerManager.removeListener(this)
        binding.playerView.player = null
        (activity as? MainActivity)?.exitFullscreen()
        _binding = null
    }

    override fun onTrackChanged(file: MediaFile?, index: Int) {
        updateNowPlaying()
    }

    override fun onPlaylistChanged(playlist: List<MediaFile>) {
        updateNowPlaying()
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        updatePlayPauseButton()
        
        if (playerManager.isCurrentVideo()) {
            if (isPlaying) {
                resetHideTimer()
            } else {
                handler.removeCallbacks(hideControlsRunnable)
                showControls()
            }
        }
    }
}
