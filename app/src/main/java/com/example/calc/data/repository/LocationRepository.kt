package com.example.calc.data.repository

import android.os.Environment
import com.example.calc.data.model.LocationData
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream

class LocationRepository {
    private val gson = Gson()
    private val locationFile = "locations.json"

    fun saveLocation(locationData: LocationData) {
        val jsonString = gson.toJson(locationData)
        try {
            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), locationFile)
            FileOutputStream(file, true).use { fos ->
                fos.write("$jsonString\n".toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getExternalFilesDir(type: String): File {
        return File(Environment.getExternalStorageDirectory(), "Android/data/com.example.calc/files/$type")
    }
}