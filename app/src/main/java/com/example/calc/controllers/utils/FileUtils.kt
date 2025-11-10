package com.example.calc.controller.utils

import java.io.File

object FileUtils {
    fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }

    fun isMusicFile(file: File): Boolean {
        val supportedExtensions = arrayOf(".mp3", ".wav", ".ogg", ".m4a", ".flac")
        val fileName = file.name.lowercase()
        return supportedExtensions.any { fileName.endsWith(it) }
    }
}