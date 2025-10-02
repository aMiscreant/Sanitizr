package com.yourname.sanitizr

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log

import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.yourname.sanitizr.model.FileItem
import com.yourname.sanitizr.ui.FileListAdapter
import com.yourname.sanitizr.util.FileSanitizer

import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: FileListAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var btnScan: Button
    private lateinit var btnSanitize: Button
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBar)
        btnScan = findViewById(R.id.btnScan)
        btnSanitize = findViewById(R.id.btnSanitize)
        recyclerView = findViewById(R.id.rvFiles)

        adapter = FileListAdapter(mutableListOf()) { selectedCount ->
            btnSanitize.isEnabled = selectedCount > 0
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnScan.setOnClickListener {
            checkStoragePermissionAndScan()
        }

        btnSanitize.setOnClickListener {
            sanitizeSelectedFiles()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            scanFiles()
        }
    }

    private fun checkStoragePermissionAndScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                scanFiles()
            } else {
                showPermissionDialog()
            }
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1001
            )
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("UseKtx")
    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Storage Permission Needed")
            .setMessage(
                "Sanitizr requires permission to access files to scan and sanitize them. " +
                        "Please allow access to all files."
            )
            .setCancelable(false)
            .setPositiveButton("Grant") { _, _ ->
                try {
                    // Open the Manage All Files Access permission page for this app
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                    // Fallback: open general settings
                    val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(fallbackIntent)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    this,
                    "Cannot scan files without permission.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun scanFiles() {
        progressBar.visibility = View.VISIBLE
        btnScan.isEnabled = false
        btnSanitize.isEnabled = false

        Thread {
            val scannedFiles = mutableListOf<FileItem>()

            // Standard Android folders to scan
            val foldersToScan = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            )

            for (dir in foldersToScan) {
                scanDirectoryRecursively(dir, scannedFiles)
            }

            runOnUiThread {
                adapter.updateFiles(scannedFiles)

                progressBar.visibility = View.GONE
                btnScan.isEnabled = true
                btnSanitize.isEnabled = false

                Toast.makeText(
                    this,
                    "Scan complete, found ${scannedFiles.size} files.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.start()
    }

    /**
     * Recursively scans a directory and adds files to scannedFiles list
     */
    private fun scanDirectoryRecursively(dir: File, scannedFiles: MutableList<FileItem>) {
        if (!dir.exists() || !dir.isDirectory) {
            Log.e("Sanitizr", "Directory does not exist or is not a directory: ${dir.absolutePath}")
            return
        }

        val files = dir.listFiles()
        if (files == null) {
            Log.e("Sanitizr", "Could not list files in ${dir.absolutePath}")
            return
        }

        Log.d("Sanitizr", "Scanning directory: ${dir.absolutePath} (${files.size} items)")
        for (file in files) {
            if (file.isFile) {
                Log.d("Sanitizr", "File found: ${file.absolutePath}")
                val fileType = determineFileType(file)
                scannedFiles.add(FileItem(file, fileType, isSanitized = false))
            } else if (file.isDirectory) {
                // Recursively scan subdirectory
                scanDirectoryRecursively(file, scannedFiles)
            }
        }
    }

    private fun sanitizeSelectedFiles() {
        val selectedFiles = adapter.getSelectedFiles()
        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, "No files selected to sanitize", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnScan.isEnabled = false
        btnSanitize.isEnabled = false

        Thread {
            var successCount = 0
            val failedFiles = mutableListOf<File>()

            for (fileItem in selectedFiles) {
                try {
                    val result = FileSanitizer.sanitizeFile(fileItem.file, fileItem.fileType)
                    if (result) {
                        successCount++
                    } else {
                        failedFiles.add(fileItem.file)
                    }
                } catch (e: Exception) {
                    Log.e("Sanitizr", "Failed to sanitize ${fileItem.file.absolutePath}", e)
                    failedFiles.add(fileItem.file)
                }
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                btnScan.isEnabled = true
                btnSanitize.isEnabled = false

                Toast.makeText(
                    this,
                    "Sanitized $successCount files." +
                            if (failedFiles.isNotEmpty()) "\nFailed: ${failedFiles.size}" else "",
                    Toast.LENGTH_LONG
                ).show()

                // Refresh list and mark sanitized files
                val updatedList = adapter.getAllFiles().map { file ->
                    if (selectedFiles.any { it.file.absolutePath == file.file.absolutePath }) {
                        file.copy(isSanitized = true)
                    } else {
                        file
                    }
                }
                adapter.updateFiles(updatedList)
            }
        }.start()
    }

    private fun determineFileType(file: File): String {
        val name = file.name.lowercase()

        return when {
            // Images
            name.endsWith(".jpg") || name.endsWith(".jpeg") -> "image"
            name.endsWith(".png") -> "image"
            name.endsWith(".gif") -> "image"
            name.endsWith(".tiff") || name.endsWith(".tif") -> "image"
            name.endsWith(".bmp") -> "image"
            name.endsWith(".webp") -> "image"
            name.endsWith(".heic") || name.endsWith(".heif") -> "image"
            name.endsWith(".raw") -> "image"
            name.endsWith(".dng") -> "image"

            // Video
            name.endsWith(".mp4") -> "video"
            name.endsWith(".mov") -> "video"
            name.endsWith(".mkv") -> "video"
            name.endsWith(".avi") -> "video"
            name.endsWith(".webm") -> "video"
            name.endsWith(".flv") -> "video"
            name.endsWith(".wmv") -> "video"
            name.endsWith(".3gp") -> "video"
            name.endsWith(".mts") || name.endsWith(".m2ts") -> "video"

            // Audio
            name.endsWith(".mp3") -> "audio"
            name.endsWith(".wav") -> "audio"
            name.endsWith(".flac") -> "audio"
            name.endsWith(".ogg") -> "audio"
            name.endsWith(".aac") -> "audio"
            name.endsWith(".m4a") -> "audio"
            name.endsWith(".wma") -> "audio"
            name.endsWith(".aiff") -> "audio"

            // Documents
            name.endsWith(".pdf") -> "pdf"
            name.endsWith(".doc") || name.endsWith(".docx") -> "document"
            name.endsWith(".xls") || name.endsWith(".xlsx") -> "document"
            name.endsWith(".ppt") || name.endsWith(".pptx") -> "document"
            name.endsWith(".odt") -> "document"
            name.endsWith(".ods") -> "document"
            name.endsWith(".txt") -> "text"
            name.endsWith(".rtf") -> "text"
            name.endsWith(".md") -> "text"
            name.endsWith(".csv") -> "text"

            // Archives (may contain file metadata or timestamps)
            name.endsWith(".zip") -> "archive"
            name.endsWith(".rar") -> "archive"
            name.endsWith(".7z") -> "archive"
            name.endsWith(".tar") -> "archive"
            name.endsWith(".gz") -> "archive"
            name.endsWith(".bz2") -> "archive"

            // Ebooks
            name.endsWith(".epub") -> "ebook"
            name.endsWith(".mobi") -> "ebook"
            name.endsWith(".azw3") -> "ebook"
            name.endsWith(".fb2") -> "ebook"

            else -> "unknown"
        }
    }

}
