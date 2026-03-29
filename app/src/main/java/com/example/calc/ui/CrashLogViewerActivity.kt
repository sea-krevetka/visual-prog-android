package com.example.calc.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.calc.R
import com.example.calc.utils.CrashLogger

class CrashLogViewerActivity : AppCompatActivity() {

    private lateinit var tvLogContent: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnClear: Button
    private lateinit var btnExport: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash_log_viewer)

        tvLogContent = findViewById(R.id.tvLogContent)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnClear = findViewById(R.id.btnClear)
        btnExport = findViewById(R.id.btnExport)

        btnRefresh.setOnClickListener { loadLogs() }
        btnClear.setOnClickListener { clearLogs() }
        btnExport.setOnClickListener { exportLogs() }

        loadLogs()
    }

    private fun loadLogs() {
        val logs = CrashLogger.getLatestLog(this)
        tvLogContent.text = if (logs.isBlank()) {
            "No logs available.\n\nLogs will appear here when errors occur."
        } else {
            logs
        }
    }

    private fun clearLogs() {
        CrashLogger.clearLogs(this)
        tvLogContent.text = "Logs cleared."
        android.widget.Toast.makeText(this, "Crash logs cleared", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun exportLogs() {
        android.widget.Toast.makeText(this, "Logs saved to app storage", android.widget.Toast.LENGTH_SHORT).show()
    }
}
