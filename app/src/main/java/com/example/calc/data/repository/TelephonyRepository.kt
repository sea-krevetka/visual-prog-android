package com.example.calc.data.repository

import android.content.Context
import android.util.Log
import com.example.calc.data.model.TelephonyData
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TelephonyRepository(private val context: Context) {
    private val gson = Gson()
    private val TAG = "TelephonyRepository"

    fun saveTelephony(telephonyData: TelephonyData) {
        val jsonString = gson.toJson(telephonyData)
        try {
            val dir = File(context.filesDir, "telephony")
            if (!dir.exists()) {
                val created = dir.mkdirs()
                Log.d(TAG, "Directory created: $created at ${dir.absolutePath}")
            }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault()).format(Date())
            val file = File(dir, "telephony_$timestamp.json")
            FileOutputStream(file).use { fos ->
                fos.write(jsonString.toByteArray())
            }
            Log.d(TAG, "Saved telemetry to ${file.absolutePath} (${file.length()} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save telemetry: ${e.message}", e)
        }
    }
}
