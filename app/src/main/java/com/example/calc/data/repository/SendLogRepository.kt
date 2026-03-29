package com.example.calc.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SendLogEntry(
    val timestamp: Long,
    val status: String,
    val payload: Map<String, Any?>,
    val attempts: Int,
    val type: String = "sent"  // "sent" or "saved"
)

class SendLogRepository(private val context: Context) {
    private val gson = Gson()
    private val file: File = File(context.filesDir, "zmq_sent_log.jsonl")

    fun addLog(status: String, payload: Map<String, Any?>, attempts: Int = 0) {
        try {
            if (!file.exists()) file.createNewFile()
            val entry = SendLogEntry(timestamp = System.currentTimeMillis(), status = status, payload = payload, attempts = attempts, type = "sent")
            FileOutputStream(file, true).use { fos ->
                fos.write((gson.toJson(entry) + "\n").toByteArray())
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun readAll(): List<SendLogEntry> {
        try {
            val allEntries = mutableListOf<SendLogEntry>()
            
            // Read sent log entries
            if (file.exists()) {
                val lines = file.readLines()
                val type = object : TypeToken<SendLogEntry>() {}.type
                for (l in lines) {
                    try {
                        val e: SendLogEntry = gson.fromJson(l, type)
                        allEntries.add(e)
                    } catch (_: Exception) {}
                }
            }
            
            // Read saved telemetry data entries
            try {
                val telemetryDir = File(context.filesDir, "telephony")
                if (telemetryDir.exists() && telemetryDir.isDirectory) {
                    val files = telemetryDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
                    for (file in files) {
                        try {
                            val json = file.readText()
                            val timestamp = extractTimestamp(file.name)
                            val entry = SendLogEntry(
                                timestamp = timestamp,
                                status = "SAVED (Local)",
                                payload = mapOf("filename" to file.name, "size" to file.length()),
                                attempts = 0,
                                type = "saved"
                            )
                            allEntries.add(entry)
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
            
            return allEntries.sortedByDescending { it.timestamp }
        } catch (t: Throwable) {
            t.printStackTrace()
            return emptyList()
        }
    }

    private fun extractTimestamp(filename: String): Long {
        return try {
            // Parse filename like "telephony_20240101_120000000.json"
            val parts = filename.replace("telephony_", "").replace(".json", "").split("_")
            if (parts.size >= 2) {
                val dateStr = "${parts[0]} ${parts[1].substring(0, 2)}:${parts[1].substring(2, 4)}:${parts[1].substring(4, 6)}"
                SimpleDateFormat("yyyyMMdd HH:mm:ss", Locale.US).parse(dateStr)?.time ?: System.currentTimeMillis()
            } else {
                System.currentTimeMillis()
            }
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    fun clear() {
        try { 
            file.writeText("") 
            // Note: Saved telemetry data (local) is NOT cleared to preserve historical data
        } catch (_: Exception) {}
    }
}
