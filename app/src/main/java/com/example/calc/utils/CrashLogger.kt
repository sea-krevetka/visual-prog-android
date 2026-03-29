package com.example.calc.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {
    private const val TAG = "CrashLogger"
    private const val CRASH_LOG_DIR = "crash_logs"
    private const val MAX_LOG_SIZE = 1024 * 1024 // 1MB

    private var context: Context? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(appContext: Context) {
        context = appContext
        setupUncaughtExceptionHandler()
    }

    private fun setupUncaughtExceptionHandler() {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            logCrash(exception)
            originalHandler?.uncaughtException(thread, exception)
        }
    }

    fun logCrash(exception: Throwable) {
        try {
            val context = context ?: return
            val logDir = File(context.getExternalFilesDir(null), CRASH_LOG_DIR)
            logDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val logFile = File(logDir, "crash_$timestamp.txt")

            FileWriter(logFile, true).use { writer ->
                writer.append("========== CRASH LOG ==========\n")
                writer.append("Timestamp: ${dateFormat.format(Date())}\n")
                writer.append("Thread: ${Thread.currentThread().name}\n")
                writer.append("Exception: ${exception.javaClass.simpleName}\n")
                writer.append("Message: ${exception.message}\n")
                writer.append("\nStackTrace:\n")
                exception.printStackTrace(java.io.PrintWriter(writer))
                writer.append("\nCause:\n")
                var cause = exception.cause
                while (cause != null) {
                    cause.printStackTrace(java.io.PrintWriter(writer))
                    cause = cause.cause
                }
                writer.append("\n================================\n\n")
            }

            // Clean old logs if too many
            cleanOldLogs(logDir)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log", e)
        }
    }

    fun logException(tag: String, message: String, exception: Throwable? = null) {
        try {
            val context = context ?: return
            val logDir = File(context.getExternalFilesDir(null), CRASH_LOG_DIR)
            logDir.mkdirs()

            val logFile = File(logDir, "app_errors.txt")

            FileWriter(logFile, true).use { writer ->
                writer.append("[${dateFormat.format(Date())}] $tag: $message\n")
                if (exception != null) {
                    exception.printStackTrace(java.io.PrintWriter(writer))
                    writer.append("\n")
                }
            }

            // Clean if file too large
            if (logFile.length() > MAX_LOG_SIZE) {
                logFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write exception log", e)
        }
    }

    fun getAllLogs(context: Context): List<String> {
        return try {
            val logDir = File(context.getExternalFilesDir(null), CRASH_LOG_DIR)
            if (!logDir.exists()) return emptyList()

            logDir.listFiles()?.mapNotNull { file ->
                if (file.isFile && (file.name.endsWith(".txt"))) {
                    file.readText()
                } else null
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read logs", e)
            emptyList()
        }
    }

    fun getLatestLog(context: Context): String {
        return try {
            val logDir = File(context.getExternalFilesDir(null), CRASH_LOG_DIR)
            if (!logDir.exists()) return "No logs available"

            val files = logDir.listFiles()?.filter { it.isFile && it.name.endsWith(".txt") }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()

            if (files.isEmpty()) {
                return "No error logs found"
            }

            val allLogs = files.joinToString("\n\n") { file ->
                "===== ${file.name} =====\n${file.readText()}"
            }
            
            allLogs.takeLast(10000) // Last 10000 chars
        } catch (e: Exception) {
            "Error reading logs: ${e.message}"
        }
    }

    fun clearLogs(context: Context) {
        try {
            val logDir = File(context.getExternalFilesDir(null), CRASH_LOG_DIR)
            logDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }

    private fun cleanOldLogs(logDir: File) {
        try {
            val files = logDir.listFiles() ?: return
            if (files.size > 20) {
                files.sortByDescending { it.lastModified() }
                files.drop(20).forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean old logs", e)
        }
    }
}
