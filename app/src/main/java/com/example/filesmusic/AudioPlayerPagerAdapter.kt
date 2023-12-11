package com.example.filesmusic

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.io.File

class AudioPlayerPagerAdapter(
    private val file: File?,
    private val singleFileMode: Boolean = false,
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int {
        // 2 tab player and file list
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            // player tab
            0 -> AudioPlayerFragment.newInstance(singleFileMode)
            // file list tab
            1 -> FileExplorerFragment.newInstance(file)
            else -> throw IllegalArgumentException("Invalid AudioPlayerPagerAdapter createFragment position.")
        }
    }
}