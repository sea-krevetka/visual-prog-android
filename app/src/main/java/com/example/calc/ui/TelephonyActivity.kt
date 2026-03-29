package com.example.calc.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.calc.data.model.TelephonyData
import com.google.gson.Gson
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.calc.R
import com.example.calc.utils.CrashLogger

class TelephonyActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_PERMISSION_CODE = 1001
        private const val PREFS_NAME = "ZmqPrefs"
        private const val PREF_HOST = "zmq_host"
        private const val PREF_SEND_ENABLED = "send_enabled"
        private const val PREF_SERVICE_RUNNING = "service_running"
    }

    private val TAG = "TelephonyActivity"
    private var isActivityVisible = false  // Track if activity is in foreground

    private lateinit var tvCellInfo: TextView
    private lateinit var etZmqHost: EditText
    private lateinit var etZmqPort: EditText
    private lateinit var btnZmqToggle: Button
    private lateinit var btnZmqUpdate: Button
    private lateinit var btnShowSent: Button
    private lateinit var btnServiceToggle: Button
    private lateinit var btnRefreshData: Button
    private lateinit var chartSignal: ImPlotSignalChartView
    private val gson = Gson()

    private val telemetryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                Log.i(TAG, "BroadcastReceiver.onReceive called, isActivityVisible=$isActivityVisible")
                if (!isActivityVisible) {
                    Log.d(TAG, "Activity not visible, ignoring broadcast")
                    return
                }
                
                if (intent?.action == TelephonyBackgroundService.ACTION_TELEMETRY_UPDATE) {
                    val json = intent.getStringExtra(TelephonyBackgroundService.EXTRA_TELEMETRY_JSON)
                    Log.d(TAG, "Received telemetry broadcast: ${json?.take(100)}...")
                    
                    json?.let {
                        try {
                            val data = gson.fromJson(it, TelephonyData::class.java)
                            Log.d(TAG, "Parsed telemetry: ${data.cellInfo.size} cells, timestamp=${data.timestamp}")
                            
                            // Update chart
                            try {
                                if (isActivityVisible && ::chartSignal.isInitialized) {
                                    chartSignal.updateFromTelephonyData(data)
                                    Log.d(TAG, "Chart updated successfully")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to update chart: ${e.message}", e)
                            }
                            
                            // Update summary text
                            if (isActivityVisible && ::tvCellInfo.isInitialized) {
                                tvCellInfo.post {
                                    tvCellInfo.text = "✓ Sample: ${data.timestamp} | ${data.cellInfo.size} cells | " +
                                        "Lat: ${String.format("%.4f", data.location?.latitude ?: 0.0)}"
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to decode telemetry JSON: ${e.message}", e)
                            if (isActivityVisible && ::tvCellInfo.isInitialized) {
                                tvCellInfo.post {
                                    tvCellInfo.text = "Error parsing telemetry: ${e.message}"
                                }
                            }
                        }
                    } ?: run {
                        Log.w(TAG, "Received null JSON in telemetry broadcast")
                        if (isActivityVisible && ::tvCellInfo.isInitialized) {
                            tvCellInfo.post {
                                tvCellInfo.text = "Error: No JSON data in broadcast"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error in BroadcastReceiver: ${e.message}", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "===== onCreate started =====")
        
        try {
            setContentView(R.layout.activity_telephony)
            Log.i(TAG, "✓ setContentView succeeded")
        } catch (e: Exception) {
            Log.e(TAG, "❌ CRASH in setContentView: ${e.javaClass.simpleName}: ${e.message}", e)
            e.printStackTrace()
            return
        }
        
        try {
            initializeViews()
            Log.i(TAG, "✓ initializeViews succeeded")
        } catch (e: Exception) {
            Log.e(TAG, "❌ CRASH in initializeViews: ${e.javaClass.simpleName}: ${e.message}", e)
            e.printStackTrace()
        }
        
        try {
            initializeZmqViews()
            Log.i(TAG, "✓ initializeZmqViews succeeded")
        } catch (e: Exception) {
            Log.e(TAG, "❌ CRASH in initializeZmqViews: ${e.javaClass.simpleName}: ${e.message}", e)
            e.printStackTrace()
        }
        
        try {
            checkPermissions()
            Log.i(TAG, "✓ checkPermissions succeeded")
        } catch (e: Exception) {
            Log.e(TAG, "❌ CRASH in checkPermissions: ${e.javaClass.simpleName}: ${e.message}", e)
            e.printStackTrace()
        }
        
        Log.i(TAG, "===== onCreate completed =====")
    }

    private fun initializeViews() {
        tvCellInfo = findViewById(R.id.tvCellInfo) ?: throw Exception("tvCellInfo view not found")
        btnServiceToggle = findViewById(R.id.btnServiceToggle) ?: throw Exception("btnServiceToggle view not found")
        chartSignal = findViewById(R.id.chartSignal) ?: throw Exception("chartSignal view not found")

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        updateServiceToggleButton(prefs)

        btnServiceToggle.setOnClickListener {
            val isRunning = prefs.getBoolean(PREF_SERVICE_RUNNING, false)
            if (isRunning) {
                stopTelephonyService(prefs)
            } else {
                startTelephonyService(prefs)
            }
        }
        Log.d(TAG, "Views initialized successfully")
    }

    private fun updateServiceToggleButton(prefs: android.content.SharedPreferences) {
        val isRunning = prefs.getBoolean(PREF_SERVICE_RUNNING, false)
        if (isRunning) {
            btnServiceToggle.text = "⏹ STOP SERVICE"
            btnServiceToggle.setBackgroundColor(android.graphics.Color.parseColor("#D32F2F"))
        } else {
            btnServiceToggle.text = "▶ START SERVICE"
            btnServiceToggle.setBackgroundColor(android.graphics.Color.parseColor("#388E3C"))
        }
    }

    private fun initializeZmqViews() {
        etZmqHost = findViewById(R.id.etZmqHost) ?: throw Exception("etZmqHost view not found")
        etZmqPort = findViewById(R.id.etZmqPort) ?: throw Exception("etZmqPort view not found")
        btnZmqToggle = findViewById(R.id.btnZmqToggle) ?: throw Exception("btnZmqToggle view not found")
        btnZmqUpdate = findViewById(R.id.btnZmqUpdate) ?: throw Exception("btnZmqUpdate view not found")
        btnShowSent = findViewById(R.id.btnShowSent) ?: throw Exception("btnShowSent view not found")
        btnRefreshData = findViewById(R.id.btnRefreshData) ?: throw Exception("btnRefreshData view not found")

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        etZmqHost.setText(prefs.getString(PREF_HOST, "192.168.62.19")) // Default to a common LAN IP
        etZmqPort.setText("2222")

        if (prefs.getBoolean(PREF_SEND_ENABLED, false)) {
            btnZmqToggle.text = "Disable Send"
        }

        btnRefreshData.setOnClickListener {
            Toast.makeText(this, "Refreshing data...", Toast.LENGTH_SHORT).show()
            loadAndDisplaySavedData()
        }

        btnZmqToggle.setOnClickListener {
            val isCurrentlyEnabled = btnZmqToggle.text.toString().contains("Disable", true)
            val host = etZmqHost.text.toString().ifBlank { "127.0.0.1" }
            val port = etZmqPort.text.toString().toIntOrNull() ?: 2222
            if (isCurrentlyEnabled) {
                // Disable send
                sendZmqCommand(false, host, port)
                btnZmqToggle.text = "Enable Send"
                with(prefs.edit()) {
                    putBoolean(PREF_SEND_ENABLED, false)
                    apply()
                }
            } else {
                // Enable send
                sendZmqCommand(true, host, port)
                btnZmqToggle.text = "Disable Send"
                with(prefs.edit()) {
                    putString(PREF_HOST, host)
                    putBoolean(PREF_SEND_ENABLED, true)
                    apply()
                }
            }
        }

        btnZmqUpdate.setOnClickListener {
            val host = etZmqHost.text.toString().ifBlank { "127.0.0.1" }
            val port = etZmqPort.text.toString().toIntOrNull() ?: 2222
            if (prefs.getBoolean(PREF_SEND_ENABLED, false)) {
                sendZmqCommand(true, host, port)
                Toast.makeText(this, "ZMQ endpoint updated: $host:$port", Toast.LENGTH_SHORT).show()
                with(prefs.edit()) {
                    putString(PREF_HOST, host)
                    apply()
                }
            } else {
                Toast.makeText(this, "Enable send first", Toast.LENGTH_SHORT).show()
            }
        }

        btnShowSent.setOnClickListener {
            startActivity(Intent(this, SentSnapshotsActivity::class.java))
        }
        
        Log.d(TAG, "ZMQ views initialized successfully")
    }

    private fun startTelephonyService(prefs: android.content.SharedPreferences) {
        try {
            val intent = Intent(this, TelephonyBackgroundService::class.java)
            intent.action = TelephonyBackgroundService.ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Starting foreground service (Android O+)")
                ContextCompat.startForegroundService(this, intent)
            } else {
                Log.d(TAG, "Starting service (Android pre-O)")
                startService(intent)
            }
            with(prefs.edit()) {
                putBoolean(PREF_SERVICE_RUNNING, true)
                apply()
            }
            updateServiceToggleButton(prefs)
            Toast.makeText(this, "Background service started", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Service start intent sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service: ${e.message}", e)
            Toast.makeText(this, "Error starting service: ${e.message}", Toast.LENGTH_SHORT).show()
            CrashLogger.logException(TAG, "Failed to start service", e)
        }
    }

    private fun stopTelephonyService(prefs: android.content.SharedPreferences) {
        try {
            val intent = Intent(this, TelephonyBackgroundService::class.java).apply {
                action = TelephonyBackgroundService.ACTION_STOP
            }
            Log.d(TAG, "Stopping service")
            startService(intent)
            with(prefs.edit()) {
                putBoolean(PREF_SERVICE_RUNNING, false)
                apply()
            }
            updateServiceToggleButton(prefs)
            Toast.makeText(this, "Background service stopped", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Service stop intent sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping service: ${e.message}", e)
            Toast.makeText(this, "Error stopping service: ${e.message}", Toast.LENGTH_SHORT).show()
            CrashLogger.logException(TAG, "Failed to stop service", e)
        }
    }

    private fun sendZmqCommand(enable: Boolean, host: String, port: Int) {
        try {
            val intent = Intent(this, TelephonyBackgroundService::class.java).apply {
                action = if (enable) TelephonyBackgroundService.ACTION_ENABLE_ZMQ else TelephonyBackgroundService.ACTION_DISABLE_ZMQ
                putExtra(TelephonyBackgroundService.EXTRA_ZMQ_HOST, host)
                putExtra(TelephonyBackgroundService.EXTRA_ZMQ_PORT, port)
            }
            Log.d(TAG, "Sending ZMQ command: ${intent.action}")
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending ZMQ command: ${e.message}", e)
        }
    }

    private fun checkPermissions() {
        try {
            val needed = mutableListOf<String>()
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.READ_PHONE_STATE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            // Android 13+ requires READ_PHONE_NUMBERS for detailed phone state
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
                    needed.add(Manifest.permission.READ_PHONE_NUMBERS)
                }
            }

            Log.d(TAG, "Permissions needed: ${needed.size}")
            if (needed.isNotEmpty()) {
                Log.d(TAG, "Requesting permissions: ${needed.joinToString()}")
                ActivityCompat.requestPermissions(this, needed.toTypedArray(), REQUEST_PERMISSION_CODE)
            } else {
                // All permissions already granted - auto-start service
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                if (!prefs.getBoolean(PREF_SERVICE_RUNNING, false)) {
                    Log.d(TAG, "Auto-starting service (all permissions granted)")
                    startTelephonyService(prefs)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkPermissions: ${e.message}", e)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            try {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Log.d(TAG, "Permissions granted - auto-starting service")
                    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    startTelephonyService(prefs)
                    Toast.makeText(this, "Permissions granted - service started", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "Required permissions not granted: ${permissions.joinToString()}")
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onRequestPermissionsResult: ${e.message}", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityVisible = true
        Log.i(TAG, "onResume started")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ requires flags for registerReceiver
                registerReceiver(
                    telemetryReceiver,
                    IntentFilter(TelephonyBackgroundService.ACTION_TELEMETRY_UPDATE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                registerReceiver(
                    telemetryReceiver,
                    IntentFilter(TelephonyBackgroundService.ACTION_TELEMETRY_UPDATE)
                )
            }
            Log.i(TAG, "Broadcast receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering receiver: ${e.message}", e)
        }
        
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                updateServiceToggleButton(prefs)
                Log.i(TAG, "Service toggle button updated")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating service button: ${e.message}", e)
        }
        
        try {
            // Load and display saved data
            loadAndDisplaySavedData()
            Log.i(TAG, "Data loading started")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading data: ${e.message}", e)
        }
        Log.i(TAG, "onResume completed")
    }
    
    private fun loadAndDisplaySavedData() {
        Thread {
            try {
                Log.d(TAG, "Loading saved telemetry data from disk, isActivityVisible=$isActivityVisible")
                val telephonyDir = java.io.File(filesDir, "telephony")
                
                if (!isActivityVisible) {
                    Log.d(TAG, "Activity not visible, stopping data load")
                    return@Thread
                }
                
                if (!telephonyDir.exists()) {
                    Log.d(TAG, "Telephony directory does not exist yet")
                    if (isActivityVisible && ::tvCellInfo.isInitialized) {
                        tvCellInfo.post {
                            tvCellInfo.text = "⏳ Waiting for data collection to start...\nEnsure service is running."
                        }
                    }
                    return@Thread
                }
                
                val files = telephonyDir.listFiles()?.filter { it.name.endsWith(".json") }?.sortedByDescending { it.lastModified() } ?: emptyList()
                Log.d(TAG, "Found ${files.size} saved telemetry files")
                
                if (files.isEmpty()) {
                    if (isActivityVisible && ::tvCellInfo.isInitialized) {
                        tvCellInfo.post {
                            tvCellInfo.text = "📂 No data collected yet.\nEnsure the background service is running and collecting data."
                        }
                    }
                    return@Thread
                }
                
                // Load all files into the chart (up to 120 points)
                val filesToLoad = files.take(120)
                var processedCount = 0
                
                filesToLoad.forEach { file ->
                    if (!isActivityVisible) {
                        Log.d(TAG, "Activity not visible, stopping load loop")
                        return@Thread
                    }
                    
                    try {
                        val json = file.readText()
                        val data = gson.fromJson(json, TelephonyData::class.java)
                        Log.d(TAG, "Loaded file: ${file.name} with ${data.cellInfo.size} cells")
                        
                        // Update chart with each file
                        try {
                            if (isActivityVisible && ::chartSignal.isInitialized) {
                                chartSignal.updateFromTelephonyData(data)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to update chart from file ${file.name}: ${e.message}")
                        }
                        processedCount++
                    } catch (e: Exception) {
                        Log.w(TAG, "Error loading file ${file.name}: ${e.message}")
                    }
                }
                
                // Update UI with summary
                val latestFile = files.firstOrNull()
                if (latestFile != null && isActivityVisible) {
                    try {
                        val json = latestFile.readText()
                        val data = gson.fromJson(json, TelephonyData::class.java)
                        
                        if (isActivityVisible && ::tvCellInfo.isInitialized) {
                            tvCellInfo.post {
                                if (!isActivityVisible) return@post
                                
                                val cellCount = data.cellInfo.size
                                val locStr = if (data.location != null) {
                                    String.format("📍 %.4f, %.4f", data.location.latitude, data.location.longitude)
                                } else "📍 No location"
                                
                                tvCellInfo.text = "✓ Latest: ${data.cellInfo.size} cells | $locStr\n" +
                                    "Files loaded: $processedCount / ${files.size}\n" +
                                    "Time: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(data.timestamp))}"
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading latest file: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadAndDisplaySavedData: ${e.message}", e)
                if (isActivityVisible && ::tvCellInfo.isInitialized) {
                    tvCellInfo.post {
                        if (!isActivityVisible) return@post
                        tvCellInfo.text = "❌ Error loading data: ${e.message}"
                    }
                }
            }
        }.start()
    }

    override fun onPause() {
        super.onPause()
        isActivityVisible = false
        try {
            unregisterReceiver(telemetryReceiver)
            Log.d(TAG, "Broadcast receiver unregistered")
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered - this is safe to ignore
            Log.d(TAG, "Receiver not registered, nothing to unregister")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}", e)
        }
    }

    override fun onStop() {
        super.onStop()
        isActivityVisible = false
        Log.d(TAG, "Activity stopped - UI updates disabled")
    }

    override fun onDestroy() {
        super.onDestroy()
        isActivityVisible = false
        try {
            unregisterReceiver(telemetryReceiver)
            Log.d(TAG, "Broadcast receiver unregistered in onDestroy")
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "Receiver was not registered in onDestroy")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver in onDestroy: ${e.message}", e)
        }
        Log.d(TAG, "Activity destroyed - resources cleaned up")
    }
}

