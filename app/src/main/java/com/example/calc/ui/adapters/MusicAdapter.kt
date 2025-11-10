package com.example.calc.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calc.R
import com.example.calc.data.model.MusicFile

class MusicAdapter(
    private val musicFiles: List<MusicFile>,
    private val onItemClick: (MusicFile) -> Unit
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvFileInfo: TextView = itemView.findViewById(R.id.tvFileInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_music_file, parent, false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val file = musicFiles[position]
        
        holder.tvFileName.text = file.name
        holder.tvFileInfo.text = "Size: ${formatFileSize(file.size)}"

        holder.itemView.setOnClickListener { onItemClick(file) }
    }

    override fun getItemCount(): Int = musicFiles.size

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }
}