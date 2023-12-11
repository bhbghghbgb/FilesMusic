package com.example.filesmusic

import android.content.Context
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.time.DurationUnit
import kotlin.time.toDuration

sealed class Helper {
    companion object {

        // look for a cover image in a list of candidate files
        // prioritize normally used cover file name, then file extension, then file name in general
        // in specified order
        // indexOf returns -1 if not found, which is even higher priority than top,
        // so manually force it to lowest priority with MAX_VALUE
        suspend fun getCommonAlbumArtFromDirectory(
            context: Context, files: List<File>
//        ): Pair<Drawable, File>? = withContext(Dispatchers.IO) {
        ): File? = withContext(Dispatchers.IO) {
            files.filter {
                it.isFile && it.extension.lowercase() in arrayOf(
                    "png", "jpg", "jpeg", "tif", "tiff"
                )
            }.minWithOrNull(compareBy<File> { file ->
                arrayOf(
                    "cover", "folder", "front", "back", "albumart", "art"
                ).indexOf(file.nameWithoutExtension.lowercase()).takeUnless { it < 0 }
                    ?: Int.MAX_VALUE
            }.thenBy { file ->
                arrayOf("png", "jpg", "jpeg", "tif", "tiff").indexOf(file.extension.lowercase())
                    .takeUnless { it < 0 } ?: Int.MAX_VALUE
            }.thenBy { it.name })?.let {
//                Pair(GlideApp.with(context).asDrawable().load(it).submit().get(), it)
                it
            }
        }

        // create FileItem objects based on file type (directory/audio/unknown)
        private suspend fun fileToFileItem(it: File, directoryAlbumArtFile: File?): FileItem =
            withContext(Dispatchers.IO) {
                if (it.isDirectory) {
                    return@withContext FileItem.DirectoryItem(it.name)
                } else if (arrayOf(
                        "mp3", "flac", "opus", "wav", "ogg", "m4a", "aac"
                    ).contains(it.extension)
                ) {
                    val mmr = MediaMetadataRetriever().apply {
                        setDataSource(it.absolutePath)
                    }
                    // obtain the audio file metadata
                    return@withContext FileItem.AudioFileItem(
                        it,
                        it.name,
                        it.parentFile?.name,
                        directoryAlbumArtFile,
                        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            ?.ifBlank { null },
                        listOfNotNull(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                            ?.ifBlank { null },
                            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                                ?.ifBlank { null }).joinToString(" · ").ifBlank { null },
                        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                            ?.ifBlank { null },
                        toDurationString(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)),

                        ).also { mmr.release() }
                } else {
                    return@withContext FileItem.UnknownFileItem(it.name)
                }
            }

        // format duration to --:--:--
        private fun toDurationString(str: String?): String {
            return toDurationString(str?.toLong())
        }

        fun toDurationString(long: Long?): String {
            return long?.let {
                (if (it < 0) "-" else "") + it.toDuration(DurationUnit.MILLISECONDS)
                    .toComponents { hours, minutes, seconds, _ ->
                        if (hours != 0L) String.format(
                            "%d:%02d:%02d", abs(hours), abs(minutes), abs(seconds)
                        ) else String.format("%d:%02d", abs(minutes), abs(seconds))
                    }
            } ?: "--:--:--"
        }

        suspend fun mapFileToFileList(
            files: List<File>, directoryAlbumArtFile: File?, progress: MutableStateFlow<Int>
        ) = files.mapIndexed() { index, file ->
            progress.value = index
            fileToFileItem(file, directoryAlbumArtFile)
        }.toList()

    }
}