package com.yourname.sanitizr.model

import java.io.File

data class FileItem(
    val file: File,
    val fileType: String,
    val isSanitized: Boolean
)
