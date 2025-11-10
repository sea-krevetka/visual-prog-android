package com.example.calc.data.repository

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.example.calc.data.model.MusicFile
import java.io.File

class MusicRepository(private val context: Context) {

    fun getMusicFiles(): List<MusicFile> {
        val musicFiles = mutableListOf<MusicFile>()
        val storageDir = Environment.getExternalStorageDirectory()
        findMusicFiles(storageDir, musicFiles)
        
        if (musicFiles.isEmpty()) {
            Toast.makeText(context, "No music files found", Toast.LENGTH_SHORT).show()
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
}