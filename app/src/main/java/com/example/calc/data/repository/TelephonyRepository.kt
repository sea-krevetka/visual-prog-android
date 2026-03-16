package com.example.calc.data.repository

import android.content.Context
import com.example.calc.data.model.TelephonyData
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TelephonyRepository(private val context: Context) {
    private val gson = Gson()

    fun saveTelephony(telephonyData: TelephonyData) {
        val jsonString = gson.toJson(telephonyData)
        try {
            val dir = File(context.filesDir, "telephony")
            if (!dir.exists()) dir.mkdirs()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault()).format(Date())
            val file = File(dir, "telephony_$timestamp.json")
            FileOutputStream(file).use { fos ->
                fos.write(jsonString.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
