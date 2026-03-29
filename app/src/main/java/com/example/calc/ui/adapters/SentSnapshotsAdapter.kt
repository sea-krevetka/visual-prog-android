package com.example.calc.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calc.R
import com.example.calc.data.repository.SendLogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SentSnapshotsAdapter : RecyclerView.Adapter<SentSnapshotsAdapter.ViewHolder>() {
    private val items = mutableListOf<SendLogEntry>()
    private val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun setItems(list: List<SendLogEntry>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_sent_snapshot, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val it = items[position]
        holder.tvTime.text = format.format(Date(it.timestamp))
        
        // Show type indicator and status
        val typeIndicator = if (it.type == "saved") "💾 SAVED" else "📤 SENT"
        holder.tvStatus.text = "$typeIndicator - ${it.status}"
        
        // For saved data, show filename instead of full payload
        if (it.type == "saved") {
            val filename = it.payload["filename"] as? String ?: "unknown"
            val size = it.payload["size"] as? Long ?: 0L
            holder.tvPayload.text = "File: $filename (${formatSize(size)})"
        } else {
            holder.tvPayload.text = it.payload.toString()
        }
        
        holder.tvAttempts.text = "${it.attempts}"
    }
    
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(v: View): RecyclerView.ViewHolder(v) {
        val tvTime: TextView = v.findViewById(R.id.tvSentTime)
        val tvStatus: TextView = v.findViewById(R.id.tvSentStatus)
        val tvPayload: TextView = v.findViewById(R.id.tvSentPayload)
        val tvAttempts: TextView = v.findViewById(R.id.tvSentAttempts)
    }
}
