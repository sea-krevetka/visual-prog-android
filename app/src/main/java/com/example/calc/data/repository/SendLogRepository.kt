package com.example.calc.data.repository

import android.content.Context
import android.util.Log
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
    private val TAG = "SendLogRepository"

    fun addLog(status: String, payload: Map<String, Any?>, attempts: Int = 0) {
        try {
            if (!file.exists()) file.createNewFile()
            val entry = SendLogEntry(timestamp = System.currentTimeMillis(), status = status, payload = payload, attempts = attempts, type = "sent")
            FileOutputStream(file, true).use { fos ->
                fos.write((gson.toJson(entry) + "\n").toByteArray())
            }
            Log.d(TAG, "Added sent log entry: $status")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to add log: ${t.message}", t)
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
                Log.d(TAG, "Loaded ${lines.size} sent log entries")
            }
            
            // Read saved telemetry data entries
            try {
                val telemetryDir = File(context.filesDir, "telephony")
                Log.d(TAG, "Reading from telemetry dir: ${telemetryDir.absolutePath}")
                
                if (telemetryDir.exists() && telemetryDir.isDirectory) {
                    val files = telemetryDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
                    Log.d(TAG, "Found ${files.size} telemetry files")
                    
                    for (file in files) {
                        try {
                            val fileContent = file.readText()
                            val timestamp = extractTimestamp(file.name)
                            val entry = SendLogEntry(
                                timestamp = timestamp,
                                status = "SAVED (Local)",
                                payload = mapOf(
                                    "filename" to file.name, 
                                    "size" to file.length(),
                                    "content" to fileContent  // Include full JSON content
                                ),
                                attempts = 0,
                                type = "saved"
                            )
                            allEntries.add(entry)
                            Log.d(TAG, "Added saved entry: ${file.name}")
                        } catch (e: Exception) {
                            Log.w(TAG, "Error processing telemetry file ${file.name}: ${e.message}")
                        }
                    }
                } else {
                    Log.d(TAG, "Telemetry directory doesn't exist or is not a directory")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading telemetry files: ${e.message}", e)
            }
            
            val allSorted = allEntries.sortedByDescending { it.timestamp }
            val lastN = allSorted.take(120)  // Limit to last 120 entries
            Log.d(TAG, "Total entries loaded: ${allEntries.size}, returning: ${lastN.size}")
            return lastN
        } catch (t: Throwable) {
            Log.e(TAG, "Error in readAll: ${t.message}", t)
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
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse timestamp from $filename: ${e.message}")
            System.currentTimeMillis()
        }
    }

    fun clear() {
        try { 
            file.writeText("")
            Log.d(TAG, "Cleared sent log")
            // Note: Saved telemetry data (local) is NOT cleared to preserve historical data
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs: ${e.message}")
        }
    }
}
