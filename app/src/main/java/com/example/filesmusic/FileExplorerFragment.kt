package com.example.filesmusic

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//private const val ARG_PARAM1 = "param1"
//private const val ARG_PARAM2 = "param2"
private const val PATH = "path"

/**
 * A simple [Fragment] subclass.
 * Use the [FileExplorerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FileExplorerFragment : Fragment() {
    // TODO: Rename and change types of parameters
//    private var param1: String? = null
//    private var param2: String? = null
    private var startedFromMainActivity: Boolean = true
    private var root: File? = null
    private var files: List<File> = emptyList()
    private var fileItems: List<FileItem>? = null

    //    private var directoryAlbumArt: Drawable? = null
    private var directoryAlbumArtFile: File? = null
    private var directoryAlbumArtJob: Job? = null
    private lateinit var explorerView: RecyclerView
    private var fileItemsMappingJob: Job? = null
    private val fileItemsMappingProgress = MutableStateFlow(0)
    private lateinit var fileItemsMappingProgressIndicator: CircularProgressIndicator
    private lateinit var fileItemsMappingProgressText: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // whether this fragment is run on the main activity (where there is a path view)
        // or in audio player activity (then get the data from AudioPlayerActivity instead
        if (activity is AudioPlayerActivity) {
            startedFromMainActivity = false
            val act = activity as AudioPlayerActivity
            root = act.root
            files = act.audioFiles
            fileItems = act.audioFileItems
//            directoryAlbumArt = act.directoryAlbumArt
            directoryAlbumArtFile = act.directoryAlbumArtFile
        } else {
            arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
                root = it.getString(PATH)?.let { path -> File(path) }
                files = root?.listFiles()?.toList() ?: emptyList()
            }
            directoryAlbumArtJob = lifecycleScope.launch {
//                Helper.getCommonAlbumArtFromDirectory(requireContext(), files)?.run {
//                    directoryAlbumArt = first
//                    directoryAlbumArtFile = second
//                }
                directoryAlbumArtFile =
                    Helper.getCommonAlbumArtFromDirectory(requireContext(), files)
            }
            fileItemsMappingJob = lifecycleScope.launch {
                directoryAlbumArtJob?.join()
                fileItems =
                    Helper.mapFileToFileList(files, directoryAlbumArtFile, fileItemsMappingProgress)
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_file_explorer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.run {
            explorerView = findViewById(R.id.fileListView)
            fileItemsMappingProgressText = findViewById(R.id.fileitem_mapping_progress_text)
            fileItemsMappingProgressIndicator =
                findViewById(R.id.fileitem_mapping_progress_indicator)
            fileItemsMappingProgressText.text = "Loading\nAlbum art"
            // set size for indicator
            fileItemsMappingProgressIndicator.max = files.size
            // fake the rotate forever effect
            fileItemsMappingProgressIndicator.startAnimation(
                AnimationUtils.loadAnimation(
                    requireContext(), R.anim.circular_rotate
                )
            )
        }
    }

    override fun onStart() {
        super.onStart()
        if (startedFromMainActivity) {
            viewLifecycleOwner.lifecycleScope.launch {
                directoryAlbumArtJob?.join()
                fileItemsMappingProgressIndicator.setProgressCompat(0, true)
                fileItemsMappingProgress.collect {
                    fileItemsMappingProgressIndicator.setProgressCompat(it, true)
                    fileItemsMappingProgressText.text =
                        "Mapping files\n$it of ${files.size}\n${"%.2f".format(it.toDouble() / files.size * 100)}%"
                }
            }
            viewLifecycleOwner.lifecycleScope.launch {
                fileItemsMappingJob?.join()
                onFileItemsMappingFinished()
            }
            return
        }
        onFileItemsMappingFinished()
    }

    override fun onDestroy() {
        super.onDestroy()
        fileItemsMappingJob?.cancel()
    }

    private fun onFileItemsMappingFinished() {
        // first let's hide the full screen loading view
        requireView().findViewById<FrameLayout>(R.id.progress_frame).visibility = View.GONE
        // whether this fragment is run on the main activity (where there is a path view)
        // or in audio player activity (then don't do anything)
        (activity as? MainActivity)?.updatePathView(root)
        if (files.isEmpty()) {
            requireView().requireViewById<TextView>(R.id.txt_directory_empty).visibility = VISIBLE
            return
        }
        renderExplorerView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("path", root?.absolutePath)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
        //         * @param param1 Parameter 1.
        //         * @param param2 Parameter 2.
         * @return A new instance of fragment FileExplorerFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
        fun newInstance(file: File?) = FileExplorerFragment().apply {
            arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
                putString(PATH, file?.absolutePath)
            }
        }
    }

    private fun renderExplorerView() {
        // if in explorer activity, navigate, or if in audio activity, send the position
        if (activity is AudioPlayerActivity) {
            explorerView.adapter = ExplorerViewAdapter(fileItems ?: emptyList(),
                AppCompatResources.getDrawable(requireContext(), R.drawable.fileitem_audiofile),
                { position, _ -> sendPositionToPlayer(position) },
                { position, _ -> showFileInfoDialog(position) },
                { getPlayingPosition() })
        } else {
            explorerView.adapter = ExplorerViewAdapter(fileItems ?: emptyList(),
                AppCompatResources.getDrawable(requireContext(), R.drawable.fileitem_audiofile),
                { position, fileItem -> navigateNextFolder(files[position], fileItem) },
                { position, _ -> showFileInfoDialog(position) },
                { -1 })
        }
    }

    private fun navigateNextFolder(file: File, fileItem: FileItem) {
        // whether this fragment is run on the main activity (where there is a need to navigate as file explorer)
        // or in audio player activity (then send the position of the file to play instead)
        if (activity !is MainActivity) {
            return
        }
        when (fileItem) {
            is FileItem.DirectoryItem -> {
                requireActivity().supportFragmentManager.beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.fragmentContainerView4, newInstance(file))
                    .setReorderingAllowed(true).addToBackStack(null).commit()
            }

            is FileItem.UnknownFileItem -> Toast.makeText(
                context, "Not Audio file (UnknownFile).", Toast.LENGTH_SHORT
            ).show()

            is FileItem.AudioFileItem -> {
                startActivity(Intent(activity, AudioPlayerActivity::class.java).apply {
                    putExtra("file", file.absolutePath)
                })
            }
        }
    }

    // for use in AudioPlayerActivity
    private fun sendPositionToPlayer(position: Int) {
        // call select new file if selected song is not the one currently playing
        (activity as? AudioPlayerActivity)?.let {
            if (it.playingPosition != position) {
                it.onPlayerFileSelect(position)

            }
        }
        (activity as AudioPlayerActivity).swipeToPlayerTab()
    }

    private fun getPlayingPosition(): Int {
        return (activity as AudioPlayerActivity).playingPosition
    }

    fun notifyItemChanged(position: Int) {
        explorerView.adapter?.notifyItemChanged(position)
    }

    // below here is for viewing file details (on long click on item)
    class FileItemInfoFragment : DialogFragment() {
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
        ): View? {
//            return super.onCreateView(inflater, container, savedInstanceState)
            // return a custom view having set Path and Info of the TextView inside it
            return inflater.inflate(R.layout.fileitem_info, container, false).apply {
                run {
                    arguments?.run {
                        getString(PATH)?.let {
                            findViewById<TextView>(R.id.fileitem_info_path).text = it
                        }
                        getString(INFO)?.let {
                            findViewById<TextView>(R.id.fileitem_info_info).text = it
                        }
                    }
                }
            }
        }

        companion object {
            const val TAG = "FileItemInfo"
            private const val PATH = "path"
            private const val INFO = "info"
            fun newInstance(path: String?, info: String?): FileItemInfoFragment {
                return FileItemInfoFragment().apply {
                    arguments = Bundle().apply {
                        putString(PATH, path)
                        putString(INFO, info)
                    }
                }
            }
        }
    }

    private fun showFileInfoDialog(position: Int) {
        // use info: FileItem string + AlbumArt field if it is an AudioFileItem, else just FileItem string
        FileItemInfoFragment.newInstance("${files.getOrNull(position)?.absolutePath}\nPosition: ${position + 1}/${files.size}",
            fileItems?.getOrNull(position)?.let { fileItem ->
                (fileItem as? FileItem.AudioFileItem)?.let { audioFileItem ->
                    if (audioFileItem.embeddedAlbumArtLoadSuccess) "${audioFileItem}\nAlbumArt: Embedded"
                    else "${audioFileItem}\nAlbumArt: ${directoryAlbumArtFile ?: "None"}"
                }?.replace(", ", ",\n") ?: fileItem.toString()
            }).show(childFragmentManager, FileItemInfoFragment.TAG)
    }
}