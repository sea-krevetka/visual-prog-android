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
    val attempts: Int
)

class SendLogRepository(private val context: Context) {
    private val gson = Gson()
    private val file: File = File(context.filesDir, "zmq_sent_log.jsonl")

    fun addLog(status: String, payload: Map<String, Any?>, attempts: Int = 0) {
        try {
            if (!file.exists()) file.createNewFile()
            val entry = SendLogEntry(timestamp = System.currentTimeMillis(), status = status, payload = payload, attempts = attempts)
            FileOutputStream(file, true).use { fos ->
                fos.write((gson.toJson(entry) + "\n").toByteArray())
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun readAll(): List<SendLogEntry> {
        try {
            if (!file.exists()) return emptyList()
            val lines = file.readLines()
            val list = mutableListOf<SendLogEntry>()
            val type = object : TypeToken<SendLogEntry>() {}.type
            for (l in lines) {
                try {
                    val e: SendLogEntry = gson.fromJson(l, type)
                    list.add(e)
                } catch (_: Exception) {}
            }
            return list.reversed() // newest first
        } catch (t: Throwable) {
            t.printStackTrace()
            return emptyList()
        }
    }

    fun clear() {
        try { file.writeText("") } catch (_: Exception) {}
    }
}
