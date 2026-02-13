package com.example.mp3player.ui.nowplaying

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mp3player.R
import com.example.mp3player.data.MediaFile
import com.example.mp3player.databinding.FragmentNowPlayingBinding
import com.example.mp3player.player.MediaPlayerManager

/**
 * Fragment showing current playlist and playback controls
 */
class NowPlayingFragment : Fragment(), MediaPlayerManager.PlaybackListener {

    private var _binding: FragmentNowPlayingBinding? = null
    private val binding get() = _binding!!

    private lateinit var playerManager: MediaPlayerManager
    private lateinit var adapter: NowPlayingAdapter
    
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updateCurrentProgress()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNowPlayingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        playerManager = MediaPlayerManager.getInstance(requireContext())
        
        setupRecyclerView()
        setupControls()
        
        playerManager.addListener(this)
        
        updateUI()
    }

    private fun setupRecyclerView() {
        adapter = NowPlayingAdapter { file, index ->
            playerManager.playFile(file)
            findNavController().navigate(R.id.nav_media_player)
        }
        
        binding.recyclerPlaylist.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPlaylist.adapter = adapter
    }

    private fun setupControls() {
        binding.btnPlayPause.setOnClickListener {
            playerManager.togglePlayPause()
        }
        
        binding.currentTrackContainer.setOnClickListener {
            if (playerManager.getPlaylist().isNotEmpty()) {
                findNavController().navigate(R.id.nav_media_player)
            }
        }
    }

    private fun updateUI() {
        val playlist = playerManager.getPlaylist()
        val currentIndex = playerManager.getCurrentIndex()
        
        if (playlist.isEmpty()) {
            binding.recyclerPlaylist.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
            binding.currentTrackContainer.visibility = View.GONE
        } else {
            binding.recyclerPlaylist.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            binding.currentTrackContainer.visibility = View.VISIBLE
            
            adapter.setPlaylist(playlist, currentIndex)
            updateCurrentTrack()
        }
        
        updatePlayPauseButton()
    }

    private fun updateCurrentTrack() {
        val currentFile = playerManager.getCurrentFile()
        
        binding.tvCurrentTitle.text = currentFile?.displayName ?: ""
        
        // Set icon based on media type
        val iconRes = if (currentFile?.isVideo == true) {
            android.R.drawable.ic_media_play
        } else {
            android.R.drawable.ic_media_play
        }
        binding.ivCurrentIcon.setImageResource(iconRes)
    }

    private fun updateCurrentProgress() {
        val currentPosition = playerManager.getCurrentPosition()
        val duration = playerManager.getDuration()
        
        val progressText = "${formatTime(currentPosition)} / ${formatTime(duration)}"
        binding.tvCurrentProgress.text = progressText
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
        updateUI()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateProgressRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playerManager.removeListener(this)
        _binding = null
    }

    // PlaybackListener implementation
    override fun onTrackChanged(file: MediaFile?, index: Int) {
        adapter.setCurrentIndex(index)
        updateCurrentTrack()
    }

    override fun onPlaylistChanged(playlist: List<MediaFile>) {
        updateUI()
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        updatePlayPauseButton()
    }
}
