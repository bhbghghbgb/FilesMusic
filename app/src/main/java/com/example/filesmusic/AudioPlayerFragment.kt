package com.example.filesmusic

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//private const val ARG_PARAM1 = "param1"
//private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AudioPlayerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AudioPlayerFragment : Fragment() {
    // TODO: Rename and change types of parameters
//    private var param1: String? = null
//    private var param2: String? = null
    private lateinit var art: ImageView
    private lateinit var title1: TextView
    private lateinit var artist: TextView
    private lateinit var album: TextView
    private lateinit var seekBarDuration: TextView
    private lateinit var currentPosition: TextView
    private var seekBarThreadRunning: Boolean = false
    private val seekBarThreadHandler: Handler = Handler.createAsync(Looper.getMainLooper())
    private lateinit var seekBar: SeekBar
    private lateinit var seekBarTip: TextView
    private var seekBarDurationAsTimeRemaining: Boolean = false
    private lateinit var playButton: ImageButton
    private var playOnResume: Boolean = true
    private var singleFileMode: Boolean = false
    private var userHoldingSeekBar: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
            playOnResume = it.getBoolean("play_on_resume", true)
            singleFileMode = it.getBoolean("SINGLE_FILE_MODE", false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_audio_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireView().run {
            art = findViewById(R.id.player_albumart)
            title1 = findViewById(R.id.txt_player_title)
            artist = findViewById(R.id.txt_player_artist)
            album = findViewById(R.id.txt_player_album)
            // for marquee
            title1.isSelected = true
            artist.isSelected = true
            album.isSelected = true
            seekBarDuration = findViewById(R.id.txt_player_seekbar_right)
            currentPosition = findViewById(R.id.txt_player_seekbar_left)
            seekBar = findViewById(R.id.seekbar_player)
            seekBarTip = findViewById(R.id.seekbar_player_tip)
            playButton = findViewById(R.id.btn_player_play)
            findViewById<ImageButton>(R.id.btn_player_rewind).setOnClickListener { onRewindClicked() }
            findViewById<ImageButton>(R.id.btn_player_play).setOnClickListener { onPlayPauseClicked() }
            findViewById<ImageButton>(R.id.btn_player_forward).setOnClickListener { onForwardClicked() }
            findViewById<ImageButton>(R.id.btn_player_playagain).setOnClickListener { onPlayAgainClicked() }
            findViewById<TextView>(R.id.txt_player_seekbar_right).setOnClickListener { onSeekBarDurationClicked() }
            art.setOnClickListener { onPlayPauseClicked() }
            val skipPrev = findViewById<ImageButton>(R.id.btn_player_skipprev)
            val skipNext = findViewById<ImageButton>(R.id.btn_player_skipnext)
            val shuffle = findViewById<ToggleButton>(R.id.btn_player_shuffle)
            val loop = findViewById<ToggleButton>(R.id.btn_player_loop)
            if (singleFileMode) {
                // remove the skip/queue control buttons
                skipPrev.isEnabled = false
                skipPrev.visibility = View.GONE
                skipNext.isEnabled = false
                skipNext.visibility = View.GONE
                shuffle.isEnabled = false
                shuffle.visibility = View.GONE
                loop.isEnabled = false
                loop.visibility = View.GONE
            } else {
                skipPrev.setOnClickListener { onSkipPrevClicked() }
                skipNext.setOnClickListener { onSkipNextClicked() }
                shuffle.setOnCheckedChangeListener { _, isChecked ->
                    (activity as? AudioPlayerActivity)?.shuffle = isChecked
                }
                loop.setOnCheckedChangeListener { _, isChecked ->
                    (activity as? AudioPlayerActivity)?.loopOne = isChecked
                }
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            var progress: Int? = null
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    this.progress = progress
                    // also set
                    (activity as AudioPlayerActivity).let {
                        val duration = it.mediaPlayerDuration().toLong()
                        val remaining = progress - duration
                        seekBarTip.text = getString(
                            R.string.seekBar_player_tip,
                            Helper.toDurationString(progress.toLong()),
                            progress,
                            Helper.toDurationString(duration),
                            duration,
                            Helper.toDurationString(remaining),
                            remaining,
                        )
                    }

                    // too laggy to seek in real time
//                    (activity as AudioPlayerActivity).onPlayerSeek(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                userHoldingSeekBar = true
                // test with some animation
                seekBarTip.animate().alpha(1f)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                userHoldingSeekBar = false
                seekBarTip.animate().alpha(0f)
                progress?.let { (activity as AudioPlayerActivity).onPlayerSeek(it) }
                seekBarLoop()
            }
        })
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
        //         * @param param1 Parameter 1.
        //         * @param param2 Parameter 2.
         * @return A new instance of fragment AudioPlayerFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
//        fun newInstance(param1: String, param2: String) = AudioPlayerFragment().apply {
//            arguments = Bundle().apply {
//                putString(ARG_PARAM1, param1)
//                putString(ARG_PARAM2, param2)
//            }
//        }
        // not able to bundle MediaPlayer lol so resort to get it from the activity
        fun newInstance(singleFileMode: Boolean?) = AudioPlayerFragment().apply {
            arguments = Bundle().apply {
                putBoolean("SINGLE_FILE_MODE", singleFileMode ?: false)
            }
        }
    }

    fun reloadMetadata() {
        (activity as AudioPlayerActivity).run {
            GlideApp.with(this).load(playingAudioFileItem)
                .placeholder(CircularProgressDrawable(this).apply {
                    setStyle(CircularProgressDrawable.LARGE)
                    start()
                }).error(
                    GlideApp.with(this@AudioPlayerFragment)
                        .load(playingAudioFileItem?.directoryAlbumArtFile)
                        .error(R.mipmap.ic_launcher_round)
                ).into(art)
//            art.setImageDrawable(
//                playingArt ?: AppCompatResources.getDrawable(
//                    requireContext(), R.mipmap.ic_launcher_round
//                )
//            )
            title1.text = playingTitle ?: "$playingFileName | <No title>"
            artist.text = playingArtist ?: "<No artist>"
            album.text = playingAlbum ?: playingParentDirName?.let {
                "$it | <No album>"
            } ?: "<No album>"
            seekBarDuration.text = Helper.toDurationString(playingDuration)
            currentPosition.text = Helper.toDurationString(0)
            seekBar.max = mediaPlayerDuration()
        }
        onPlayStateChanged()
    }

    @Synchronized
    fun onPlayStateChanged() {
        if ((activity as AudioPlayerActivity).isMediaPlayerPlaying() != seekBarThreadRunning) {
            // stop/pause -> start/resume
            if (!seekBarThreadRunning) {
                requireActivity().runOnUiThread(object : Runnable {
                    override fun run() {
                        if ((activity as AudioPlayerActivity).run { isFinishing || finishImminent }) {
                            return
                        }
                        seekBarLoop()
                        seekBarThreadHandler.postDelayed(
                            this,
                            (1000 - (activity as AudioPlayerActivity).mediaPlayerPosition() / 1001).toLong()
                        )
                    }
                })
                playButton.setImageDrawable(
                    AppCompatResources.getDrawable(
                        requireContext(), R.drawable.player_pause
                    )
                )
                art.animate().setInterpolator(AccelerateInterpolator()).scaleX(1f).scaleY(1f)
                    .alpha(1f)
                seekBarThreadRunning = true
                // start/resume -> stop/pause
            } else {
                seekBarThreadHandler.removeCallbacksAndMessages(null)
                playButton.setImageDrawable(
                    AppCompatResources.getDrawable(
                        requireContext(), R.drawable.player_play
                    )
                )
                art.animate().setInterpolator(AccelerateInterpolator()).scaleX(.95f).scaleY(.95f)
                    .alpha(.8f)
                seekBarThreadRunning = false
            }
        }
        seekBarLoop()
    }

    fun seekBarLoop() {
        (activity as AudioPlayerActivity).let { act ->
            act.mediaPlayerPosition().let {
                currentPosition.text = Helper.toDurationString(it.toLong())
                if (seekBarDurationAsTimeRemaining) {
                    seekBarDuration.text = Helper.toDurationString(
                        it - act.mediaPlayerDuration().toLong()
                    )
                }

                if (!userHoldingSeekBar) {
                    seekBar.progress = it
                }
            }

        }
    }

    private fun onSkipPrevClicked() {
        (activity as AudioPlayerActivity).onPlayerSkipPrev()
        reloadMetadata()
    }

    private fun onRewindClicked() {
        (activity as AudioPlayerActivity).onPlayerRewind()
        seekBarLoop()
    }

    private fun onPlayPauseClicked() {
        if (seekBarThreadRunning) {
            (activity as AudioPlayerActivity).onPlayerPause()
        } else {
            (activity as AudioPlayerActivity).onPlayerResume()
        }
        onPlayStateChanged()
    }

    private fun onForwardClicked() {
        (activity as AudioPlayerActivity).onPlayerForward()
        seekBarLoop()
    }

    private fun onSkipNextClicked() {
        (activity as AudioPlayerActivity).onPlayerSkipNext()
        reloadMetadata()
    }

    private fun onPlayAgainClicked() {
        (activity as AudioPlayerActivity).onPlayerPlayAgain()
        seekBarLoop()
    }

    private fun onSeekBarDurationClicked() {
        seekBarDurationAsTimeRemaining = !seekBarDurationAsTimeRemaining
        if (!seekBarDurationAsTimeRemaining) {
            seekBarDuration.text = Helper.toDurationString(
                (activity as AudioPlayerActivity).mediaPlayerDuration().toLong()
            )
        }
        seekBarLoop()
    }

    override fun onResume() {
        super.onResume()
        if (playOnResume) {
            viewLifecycleOwner.lifecycleScope.launch {
                (activity as AudioPlayerActivity).waitForActivityReady()
                // activity may be finished at this time due to No Files
                if ((activity as AudioPlayerActivity).isFinishing) {
                    return@launch
                }
                // select first file to play first
                (activity as AudioPlayerActivity).onPlayerFileSelect(0)
                reloadMetadata()
                playOnResume = false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("play_on_resume", playOnResume)
        outState.putBoolean("SINGLE_FILE_MODE", singleFileMode)
    }

    override fun onDestroy() {
        super.onDestroy()
        seekBarThreadHandler.removeCallbacksAndMessages(null)
    }
}