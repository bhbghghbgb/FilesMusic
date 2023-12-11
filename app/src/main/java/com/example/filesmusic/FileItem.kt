package com.example.filesmusic

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import java.io.File

sealed class FileItem {
    data class DirectoryItem(val dirName: String?) : FileItem()
    data class UnknownFileItem(val fileName: String?) : FileItem()
    data class AudioFileItem(
        val file: File,
        val fileName: String,
        val parentDirName: String?,
        val directoryAlbumArtFile: File?,
        val trackTitle: String?,
        val artistAlbum: String?,
        val album: String?,
        val trackLength: String?
    ) : FileItem() {
        var embeddedAlbumArtLoadSuccess: Boolean = false
    }

    class DirectoryViewHolder(itemView: View) : ViewHolder(itemView) {
        val directoryName: TextView = itemView.findViewById(R.id.fileItem_name)

        init {
            // needed for marquee
            directoryName.isSelected = true
        }
    }

    class UnknownFileViewHolder(itemView: View) : ViewHolder(itemView) {
        val fileName: TextView = itemView.findViewById(R.id.fileItem_name)

        init {
            // needed for marquee
            fileName.isSelected = true
        }
    }

    class AudioFileViewHolder(itemView: View) : ViewHolder(itemView) {
        val albumArt: ImageView = itemView.findViewById(R.id.fileItem_albumart)
        val trackTitle: TextView = itemView.findViewById(R.id.fileItem_tracktitle)
        val artistAlbum: TextView = itemView.findViewById(R.id.fileItem_trackartist)
        val trackLength: TextView = itemView.findViewById(R.id.fileItem_tracklength)
        val album: TextView = itemView.findViewById(R.id.fileItem_trackalbum)
        private val originalBackground: Drawable = itemView.background

        fun setHighlight(isHighlighted: Boolean) {
            if (isHighlighted) {
                itemView.setBackgroundResource(R.drawable.border_audiofile_selecteditem)
            } else {
                itemView.background = originalBackground
            }
        }
    }
}