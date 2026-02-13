package com.example.mp3player.ui.nowplaying

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mp3player.R
import com.example.mp3player.data.MediaFile

/**
 * Adapter for displaying the current playlist
 */
class NowPlayingAdapter(
    private val onItemClick: (MediaFile, Int) -> Unit
) : RecyclerView.Adapter<NowPlayingAdapter.ViewHolder>() {

    private var playlist: List<MediaFile> = emptyList()
    private var currentIndex: Int = -1

    fun setPlaylist(files: List<MediaFile>, currentlyPlaying: Int = -1) {
        playlist = files
        currentIndex = currentlyPlaying
        notifyDataSetChanged()
    }

    fun setCurrentIndex(index: Int) {
        val oldIndex = currentIndex
        currentIndex = index
        if (oldIndex >= 0) notifyItemChanged(oldIndex)
        if (currentIndex >= 0) notifyItemChanged(currentIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = playlist[position]
        val isPlaying = position == currentIndex
        holder.bind(file, position, isPlaying)
        holder.itemView.setOnClickListener { onItemClick(file, position) }
    }

    override fun getItemCount(): Int = playlist.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIndicator: ImageView = itemView.findViewById(R.id.iv_playing_indicator)
        private val tvNumber: TextView = itemView.findViewById(R.id.tv_track_number)
        private val tvName: TextView = itemView.findViewById(R.id.tv_file_name)
        private val tvType: TextView = itemView.findViewById(R.id.tv_type)

        fun bind(file: MediaFile, position: Int, isPlaying: Boolean) {
            tvNumber.text = (position + 1).toString()
            tvName.text = file.name
            tvType.text = file.extension.uppercase()
            
            // Show playing indicator
            ivIndicator.visibility = if (isPlaying) View.VISIBLE else View.INVISIBLE
            
            // Highlight current track
            val bgColor = if (isPlaying) 0x33e94560.toInt() else 0x00000000
            itemView.setBackgroundColor(bgColor)
            
            // Text color for current track
            val textColor = if (isPlaying) 0xFFe94560.toInt() else 0xFFffffff.toInt()
            tvName.setTextColor(textColor)
        }
    }
}
