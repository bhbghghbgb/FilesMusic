package com.example.filesmusic

import java.io.File

interface ActivityWithFileList {
    fun getFilesAndFileItems(): Pair<List<File>, List<FileItem>>
}