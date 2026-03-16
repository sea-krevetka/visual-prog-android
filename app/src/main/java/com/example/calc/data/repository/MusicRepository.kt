package com.example.calc.data.repository

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.example.calc.R
import com.example.calc.data.model.MusicFile
import java.io.File

class MusicRepository(private val context: Context) {

    fun getMusicFiles(): List<MusicFile> {
        val musicFiles = mutableListOf<MusicFile>()
        val storageDir = Environment.getExternalStorageDirectory()
        findMusicFiles(storageDir, musicFiles)

        if (musicFiles.isEmpty()) {
            // If no files are found on the device, load from raw resources.
            val rawMusicFiles = getRawMusicFiles()
            if (rawMusicFiles.isEmpty()) {
                Toast.makeText(context, "No music files found on device or in res/raw", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No music on device, loading from app resources.", Toast.LENGTH_LONG).show()
            }
            return rawMusicFiles
        }

        return musicFiles
    }

    private fun findMusicFiles(directory: File, musicFiles: MutableList<MusicFile>) {
        try {
            if (directory.exists() && directory.isDirectory) {
                directory.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        findMusicFiles(file, musicFiles)
                    } else if (isMusicFile(file)) {
                        musicFiles.add(MusicFile(file))
                    }
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Cannot access directory: ${directory.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isMusicFile(file: File): Boolean {
        val supportedExtensions = arrayOf(".mp3", ".wav", ".ogg", ".m4a", ".flac")
        val fileName = file.name.lowercase()
        return supportedExtensions.any { fileName.endsWith(it) }
    }

    private fun getRawMusicFiles(): List<MusicFile> {
        val rawMusicFiles = mutableListOf<MusicFile>()
        try {
            val fields = R.raw::class.java.fields
            fields.forEach { field ->
                val resourceId = field.getInt(null)
                val resourceName = context.resources.getResourceEntryName(resourceId)
                val path = "android.resource://${context.packageName}/$resourceId"
                rawMusicFiles.add(
                    MusicFile(
                        file = null,
                        name = resourceName,
                        path = path,
                        size = 0L, // Size is not available for raw resources
                        isFromRaw = true
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error loading music from raw resources.", Toast.LENGTH_SHORT).show()
        }
        return rawMusicFiles
    }
}
