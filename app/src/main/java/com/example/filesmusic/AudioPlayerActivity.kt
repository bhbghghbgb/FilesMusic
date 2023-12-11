package com.example.filesmusic

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class AudioPlayerActivity : AppCompatActivity() {
    private val activityInstanceMediaPlayer: MediaPlayer by lazy {
        MediaPlayer()
    }
    private var filePlaySingleFile: Boolean = false
    var root: File? = null
    private var files: List<File> = emptyList()
    private var fileItems: List<FileItem> = emptyList()
    var audioFiles: List<File> = emptyList()
    var audioFileItems: List<FileItem.AudioFileItem> = emptyList()

    //    var directoryAlbumArt: Drawable? = null
    var directoryAlbumArtFile: File? = null
    private var directoryAlbumArtJob: Job? = null
    private lateinit var fileItemsMappingJob: Job
    private val fileItemsMappingProgress = MutableStateFlow(0)
    private lateinit var fileItemsMappingProgressIndicator: CircularProgressIndicator
    private lateinit var fileItemsMappingProgressText: TextView
    var playingAudioFileItem: FileItem.AudioFileItem? = null
    var playingFileName: String? = null

    //    var playingArt: Drawable? = null
    var playingTitle: String? = null
    var playingArtist: String? = null
    var playingAlbum: String? = null
    var playingParentDirName: String? = null
    var playingDuration: Long = -1
    var playingPosition: Int = -1
    var shuffle: Boolean = false
    var loopOne: Boolean = false
    var finishImminent: Boolean = false
    private lateinit var viewPager: ViewPager2
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_player)
        activityInstanceMediaPlayer.setOnCompletionListener { onTrackFinished() }
        intent?.getStringExtra("path")?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
            root = File(it)
            files = root!!.listFiles()?.toList() ?: emptyList()
            loadDirectoryAlbumArt(files)
        }
        if (files.isEmpty()) {
            intent?.getStringExtra("file")?.let {
                val file = File(it)
                root = file.parentFile
                files = listOf(file)
                root!!.listFiles()?.toList()?.let { files ->
                    loadDirectoryAlbumArt(files)
                }
            }
        }
        setupProgressIndicator()
        fileItemsMappingJob = lifecycleScope.launch {
            directoryAlbumArtJob?.join()
            fileItems =
                Helper.mapFileToFileList(files, directoryAlbumArtFile, fileItemsMappingProgress)
            onFileItemsMappingFinished()
            setupViewPager()
        }
    }

    private fun loadDirectoryAlbumArt(files: List<File>) {
        directoryAlbumArtJob = lifecycleScope.launch {
//            Helper.getCommonAlbumArtFromDirectory(this@AudioPlayerActivity, files)?.run {
//                directoryAlbumArt = first
//                directoryAlbumArtFile = second
//            }
            directoryAlbumArtFile =
                Helper.getCommonAlbumArtFromDirectory(this@AudioPlayerActivity, files)
        }
    }

    private fun onFileItemsMappingFinished() {
        // first let's hide the full screen loading view
        findViewById<FrameLayout>(R.id.progress_frame).visibility = View.GONE
        // get fileItems that are audio only
        audioFileItems = fileItems.filterIsInstance<FileItem.AudioFileItem>()
        audioFiles =
            (files.indices).filter { fileItems[it] is FileItem.AudioFileItem }.map { files[it] }
        // if there are no audio files then exit prematurely
        if (audioFiles.isEmpty() || audioFileItems.isEmpty()) {
            finishPlayerNoFiles()
        }
        // if only one file is available then use single file mode
        filePlaySingleFile = audioFileItems.size == 1
        supportActionBar?.title =
            "${getString(R.string.app_name)} (${if (filePlaySingleFile) "Single Player" else "Multi Player"})"
    }

    private fun setupViewPager() {
        viewPager = findViewById(R.id.viewPager_player)
        viewPager.adapter =
            AudioPlayerPagerAdapter(root, filePlaySingleFile, supportFragmentManager, lifecycle)
        TabLayoutMediator(findViewById(R.id.tabLayout_player), viewPager) { tab, position ->
            when (position) {
                0 -> tab.run {
                    icon = AppCompatResources.getDrawable(
                        this@AudioPlayerActivity, R.drawable.player_tab_player
                    )
                    text = "Player"
                }

                1 -> tab.run {
                    icon = AppCompatResources.getDrawable(
                        this@AudioPlayerActivity, R.drawable.player_tab_filelist
                    )
                    text = "Playlist"
                }
            }
        }.attach()
    }

    private fun setupProgressIndicator() {
        fileItemsMappingProgressIndicator = findViewById(R.id.fileitem_mapping_progress_indicator)
        fileItemsMappingProgressText = findViewById(R.id.fileitem_mapping_progress_text)
        fileItemsMappingProgressText.text = "Loading\nAlbum art"
        // set size for indicator
        fileItemsMappingProgressIndicator.max = files.size
        // fake the rotate forever effect
        fileItemsMappingProgressIndicator.startAnimation(
            AnimationUtils.loadAnimation(
                this, R.anim.circular_rotate
            )
        )
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            directoryAlbumArtJob?.join()
            fileItemsMappingProgressIndicator.setProgressCompat(0, true)
            fileItemsMappingProgress.collect {
                fileItemsMappingProgressIndicator.setProgressCompat(it, true)
                fileItemsMappingProgressText.text =
                    "Mapping files\n$it of ${files.size}\n${"%.2f".format(it.toDouble() / files.size * 100)}%"
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (files.size > 1) {
            outState.putString("path", root?.absolutePath)
        } else {
            outState.putString("file", files.firstOrNull()?.absolutePath)
        }
    }

    override fun onDestroy() {
        // dispose of the mediaplayer
        super.onDestroy()
        directoryAlbumArtJob?.cancel()
        fileItemsMappingJob.cancel()
        finishImminent = true
        activityInstanceMediaPlayer.release()
    }

    private fun finishPlayerNoFiles() {
        Toast.makeText(this, "No audio files.", Toast.LENGTH_SHORT).show()
        finish()
    }

    fun onPlayerFileSelect(position: Int) {
        if (audioFiles.isEmpty() || position !in audioFiles.indices) {
            return
        }
        // ViewPager2 FragmentManager tag hack
        // notify the explorer of the previous and this item that their highlight has changed
        // there are better ways but this is to get it working
        (supportFragmentManager.findFragmentByTag("f1") as? FileExplorerFragment)?.run {
            notifyItemChanged(playingPosition)
            notifyItemChanged(position)
        }
        playingPosition = position
        onPlayerPlay()
        // Just simply forces it to reload metadata
        // there are better ways but this is to get it working
        (supportFragmentManager.findFragmentByTag("f0") as AudioPlayerFragment).reloadMetadata()
    }

    private fun onPlayerPlay() {
        activityInstanceMediaPlayer.reset()
        try {
            activityInstanceMediaPlayer.setDataSource(audioFiles[playingPosition].absolutePath)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "IOException, check log.", Toast.LENGTH_SHORT).show()
            return
        }
        activityInstanceMediaPlayer.prepare()
        audioFileItems[playingPosition].run {
            playingAudioFileItem = this
            playingFileName = fileName
            // image is definitely already preloaded by Glide
//            playingArt =
//                runBlocking { Helper.loadAlbumArtFromAudioFile(this@AudioPlayerActivity, this@run) }
//                    ?: this@AudioPlayerActivity.directoryAlbumArt
            playingTitle = trackTitle
            playingArtist = artistAlbum
            playingAlbum = album
            playingParentDirName = parentDirName
            playingDuration = activityInstanceMediaPlayer.duration.toLong()
        }
        onPlayerResume()
    }

    fun onPlayerSkipPrev() {
        onPlayerFileSelect(
            if (shuffle) audioFiles.indices.random() else when (playingPosition) {
                0 -> audioFiles.size - 1
                else -> playingPosition - 1
            }
        )
    }

    fun onPlayerSeek(position: Int) {
        activityInstanceMediaPlayer.run { seekTo(position) }
    }

    fun onPlayerRewind() {
        onPlayerSeek(activityInstanceMediaPlayer.currentPosition - 10_000)
    }

    fun onPlayerResume() {
        activityInstanceMediaPlayer.start()
    }

    fun onPlayerPause() {
        activityInstanceMediaPlayer.pause()
    }

    fun onPlayerForward() {
        onPlayerSeek(activityInstanceMediaPlayer.currentPosition + 10_000)
    }

    fun onPlayerSkipNext() {
        onPlayerFileSelect(
            if (shuffle) audioFiles.indices.random() else when (playingPosition) {
                audioFiles.size - 1 -> 0
                else -> playingPosition + 1
            }
        )
    }

    fun onPlayerPlayAgain() {
        activityInstanceMediaPlayer.run {
            seekTo(0)
            start()
        }

    }

    private fun onTrackFinished() {
        if (loopOne) onPlayerPlay() else onPlayerSkipNext()
    }

    fun isMediaPlayerPlaying(): Boolean {
        return activityInstanceMediaPlayer.isPlaying
    }

    fun mediaPlayerPosition(): Int {
        return activityInstanceMediaPlayer.currentPosition
    }

    fun mediaPlayerDuration(): Int {
        return activityInstanceMediaPlayer.duration
    }

    fun swipeToPlayerTab() {
        viewPager.currentItem = 0
    }

    suspend fun waitForActivityReady() {
        fileItemsMappingJob.join()
    }
}