package com.yourname.sanitizr.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yourname.sanitizr.R
import com.yourname.sanitizr.model.FileItem

class FileListAdapter(
    private val files: MutableList<FileItem>,
    private val onSelectionChanged: (selectedCount: Int) -> Unit
) : RecyclerView.Adapter<FileListAdapter.FileViewHolder>() {

    // Tracks selected positions
    private val selectedPositions = mutableSetOf<Int>()

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.ivFileIcon)
        val name: TextView = itemView.findViewById(R.id.tvFileName)
        val checkbox: CheckBox = itemView.findViewById(R.id.cbSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun getItemCount(): Int = files.size

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileItem = files[position]

        // Placeholder icon based on fileType (implement real icons later)
        holder.icon.setImageResource(R.drawable.ic_file)  

        holder.name.text = fileItem.file.name
        holder.checkbox.isChecked = selectedPositions.contains(position)

        holder.checkbox.setOnCheckedChangeListener(null) // prevent recursive calls
        holder.checkbox.isChecked = selectedPositions.contains(position)
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedPositions.add(position)
            } else {
                selectedPositions.remove(position)
            }
            onSelectionChanged(selectedPositions.size)
        }

        // Clicking whole item toggles checkbox
        holder.itemView.setOnClickListener {
            val newState = !holder.checkbox.isChecked
            holder.checkbox.isChecked = newState
        }
    }

    fun updateFiles(newFiles: List<FileItem>) {
        files.clear()
        files.addAll(newFiles)
        selectedPositions.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    fun getSelectedFiles(): List<FileItem> =
    selectedPositions.map { files[it] }

    fun getAllFiles(): List<FileItem> =
        files.toList()

}

