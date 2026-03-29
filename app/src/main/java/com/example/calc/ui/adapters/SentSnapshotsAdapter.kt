package com.example.calc.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calc.R
import com.example.calc.data.repository.SendLogEntry
import com.google.gson.JsonParser
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
        
        // For saved data, parse and display the collected data
        if (it.type == "saved") {
            val content = it.payload["content"] as? String ?: ""
            if (content.isNotEmpty()) {
                try {
                    val json = JsonParser.parseString(content).asJsonObject
                    
                    // Extract key information
                    val cellCount = json.getAsJsonArray("cellInfo")?.size() ?: 0
                    val location = json.getAsJsonObject("location")
                    val traffic = json.getAsJsonObject("traffic")
                    
                    val locationStr = if (location != null) {
                        val lat = location.get("latitude")?.asDouble ?: 0.0
                        val lon = location.get("longitude")?.asDouble ?: 0.0
                        String.format("📍 (%.4f, %.4f)", lat, lon)
                    } else "📍 No location"
                    
                    val trafficStr = if (traffic != null) {
                        val rxBytes = traffic.get("totalRxBytes")?.asLong ?: 0
                        val txBytes = traffic.get("totalTxBytes")?.asLong ?: 0
                        "📊 RX: ${formatBytes(rxBytes)} TX: ${formatBytes(txBytes)}"
                    } else "📊 No traffic"
                    
                    holder.tvPayload.text = "📡 Cells: $cellCount | $locationStr\n$trafficStr"
                } catch (e: Exception) {
                    holder.tvPayload.text = content.take(300)
                }
            } else {
                val filename = it.payload["filename"] as? String ?: "unknown"
                val size = it.payload["size"] as? Long ?: 0L
                holder.tvPayload.text = "File: $filename (${formatSize(size)})"
            }
        } else {
            holder.tvPayload.text = it.payload.toString()
        }
        
        holder.tvAttempts.text = "${it.attempts}"
    }

    override fun getItemCount(): Int = items.size

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }

    class ViewHolder(v: View): RecyclerView.ViewHolder(v) {
        val tvTime: TextView = v.findViewById(R.id.tvSentTime)
        val tvStatus: TextView = v.findViewById(R.id.tvSentStatus)
        val tvPayload: TextView = v.findViewById(R.id.tvSentPayload)
        val tvAttempts: TextView = v.findViewById(R.id.tvSentAttempts)
    }
}
