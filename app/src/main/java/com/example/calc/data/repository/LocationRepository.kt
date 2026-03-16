package com.example.calc.data.repository

import android.content.Context
import com.example.calc.data.model.LocationData
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationRepository(private val context: Context) {
    private val gson = Gson()

    fun saveLocation(locationData: LocationData) {
        val jsonString = gson.toJson(locationData)
        try {
            val dir = File(context.filesDir, "lla")
            if (!dir.exists()) dir.mkdirs()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault()).format(Date())
            val file = File(dir, "lla_$timestamp.json")
            FileOutputStream(file).use { fos ->
                fos.write(jsonString.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLastLocation(): LocationData? {
        try {
            val dir = File(context.filesDir, "lla")
            if (!dir.exists()) return null
            val files = dir.listFiles { f -> f.isFile && f.name.startsWith("lla_") }
            if (files == null || files.isEmpty()) return null
            val latest = files.maxByOrNull { it.lastModified() } ?: return null
            val json = latest.readText()
            return gson.fromJson(json, LocationData::class.java)
        } catch (t: Throwable) {
            t.printStackTrace()
            return null
        }
    }
}