package com.yourname.sanitizr.util

import java.io.File

object FileUtils {
    fun listFilesInDirectory(dir: File): List<File> {
        return dir.listFiles()?.toList() ?: emptyList()
    }
}
