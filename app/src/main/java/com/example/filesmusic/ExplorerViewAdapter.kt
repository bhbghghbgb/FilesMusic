package com.example.filesmusic

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.swiperefreshlayout.widget.CircularProgressDrawable

class ExplorerViewAdapter(
    private val fileItems: List<FileItem>,
    private val defaultCover: Drawable?,
    private val fileItemClickHandler: (Int, FileItem) -> Unit,
    private val fileItemLongClickHandler: (Int, FileItem) -> Unit,
    private val getPlayingPosition: () -> Int
) : Adapter<ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when (fileItems[position]) {
            is FileItem.DirectoryItem -> R.layout.fileitem_directory
            is FileItem.UnknownFileItem -> R.layout.fileitem_unknownfile
            is FileItem.AudioFileItem -> R.layout.fileitem_audiofile
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.fileitem_directory -> {
                FileItem.DirectoryViewHolder(
                    inflater.inflate(
                        R.layout.fileitem_directory, parent, false
                    )
                )
            }

            R.layout.fileitem_unknownfile -> {
                FileItem.UnknownFileViewHolder(
                    inflater.inflate(
                        R.layout.fileitem_unknownfile, parent, false
                    )
                )
            }

            R.layout.fileitem_audiofile -> {
                FileItem.AudioFileViewHolder(
                    inflater.inflate(
                        R.layout.fileitem_audiofile, parent, false
                    )
                )
            }

            else -> throw IllegalArgumentException("Invalid view type for ExplorerView")
        }
    }

    override fun getItemCount(): Int {
        return fileItems.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fileItem = fileItems[position]
        when (holder) {
            is FileItem.DirectoryViewHolder -> {
                holder.directoryName.text = (fileItem as FileItem.DirectoryItem).dirName
            }

            is FileItem.UnknownFileViewHolder -> {
                holder.fileName.text = (fileItem as FileItem.UnknownFileItem).fileName
            }

            is FileItem.AudioFileViewHolder -> {
                val audioFileItem = fileItem as FileItem.AudioFileItem
                val context = holder.albumArt.context
                glideToAlbumArt(context, audioFileItem, holder)
                holder.trackTitle.text =
                    audioFileItem.trackTitle ?: "${audioFileItem.fileName} | <No title>"
                holder.artistAlbum.text = audioFileItem.artistAlbum ?: "<No artist>"
                holder.trackLength.text = audioFileItem.trackLength ?: "--:--:--"
                holder.album.text = audioFileItem.album ?: audioFileItem.parentDirName?.let {
                    "${audioFileItem.parentDirName} | <No album>"
                } ?: "<No album>"
                // set highlight if file is playing (getPlayingPosition should return -1 so nothing matches
                // when explorer is used in MainActivity)
                holder.setHighlight(getPlayingPosition() == position)
            }
        }
        holder.itemView.run {
            setOnClickListener { fileItemClickHandler(position, fileItem) }
            setOnLongClickListener {
                fileItemLongClickHandler(position, fileItem)
                true
            }
        }
    }

    private fun glideToAlbumArt(
        context: Context,
        audioFileItem: FileItem.AudioFileItem,
        holder: FileItem.AudioFileViewHolder
    ) {
        //                audioFileItem.albumArt?.let {
//                    holder.albumArt.setImageBitmap(it)
//                } ?: holder.albumArt.setImageDrawable(defaultCover)
        GlideApp.with(context).load(audioFileItem)
            .placeholder(CircularProgressDrawable(context).apply {
                setStyle(CircularProgressDrawable.LARGE)
                start()
            })
            .error(
                GlideApp.with(context).load(audioFileItem.directoryAlbumArtFile).error(defaultCover)
            ).into(holder.albumArt)
    }

}