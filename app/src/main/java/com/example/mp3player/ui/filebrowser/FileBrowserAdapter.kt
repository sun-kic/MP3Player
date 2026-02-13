package com.example.mp3player.ui.filebrowser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mp3player.R
import com.example.mp3player.data.MediaFile
import com.example.mp3player.data.MediaFolder

/**
 * Adapter for displaying folders and media files in a RecyclerView
 */
class FileBrowserAdapter(
    private val onFolderClick: (MediaFolder) -> Unit,
    private val onFileClick: (MediaFile) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Any>()
    
    companion object {
        private const val TYPE_FOLDER = 0
        private const val TYPE_FILE = 1
    }

    fun setItems(folders: List<MediaFolder>, files: List<MediaFile>) {
        items.clear()
        items.addAll(folders)
        items.addAll(files)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is MediaFolder -> TYPE_FOLDER
            is MediaFile -> TYPE_FILE
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_FOLDER -> {
                val view = inflater.inflate(R.layout.item_folder, parent, false)
                FolderViewHolder(view)
            }
            TYPE_FILE -> {
                val view = inflater.inflate(R.layout.item_file, parent, false)
                FileViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FolderViewHolder -> {
                val folder = items[position] as MediaFolder
                holder.bind(folder)
                holder.itemView.setOnClickListener { onFolderClick(folder) }
            }
            is FileViewHolder -> {
                val file = items[position] as MediaFile
                holder.bind(file)
                holder.itemView.setOnClickListener { onFileClick(file) }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    /**
     * ViewHolder for folder items
     */
    class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_folder_icon)
        private val tvName: TextView = itemView.findViewById(R.id.tv_folder_name)
        private val tvCount: TextView = itemView.findViewById(R.id.tv_file_count)

        fun bind(folder: MediaFolder) {
            tvName.text = folder.name
            tvCount.text = itemView.context.getString(R.string.media_files_count, folder.mediaFileCount)
        }
    }

    /**
     * ViewHolder for media file items
     */
    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_media_icon)
        private val tvName: TextView = itemView.findViewById(R.id.tv_file_name)
        private val tvSize: TextView = itemView.findViewById(R.id.tv_file_size)
        private val tvType: TextView = itemView.findViewById(R.id.tv_media_type)

        fun bind(file: MediaFile) {
            tvName.text = file.name
            tvSize.text = file.formattedSize
            tvType.text = file.extension.uppercase()
            
            // Set different tint for audio vs video
            val tintColor = if (file.isVideo) {
                0xFFe94560.toInt() // Red for video
            } else {
                0xFF4CAF50.toInt() // Green for audio
            }
            ivIcon.setColorFilter(tintColor)
        }
    }
}
