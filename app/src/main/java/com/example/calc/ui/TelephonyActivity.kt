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

class TelephonyActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_PERMISSION_CODE = 1001
        private const val PREFS_NAME = "ZmqPrefs"
        private const val PREF_HOST = "zmq_host"
        private const val PREF_SEND_ENABLED = "send_enabled"
        private const val PREF_SERVICE_RUNNING = "service_running"
    }

    private val TAG = "TelephonyActivity"

    private lateinit var tvCellInfo: TextView
    private lateinit var etZmqHost: EditText
    private lateinit var etZmqPort: EditText
    private lateinit var btnZmqToggle: Button
    private lateinit var btnZmqUpdate: Button
    private lateinit var btnShowSent: Button
    private lateinit var btnServiceToggle: Button
    private lateinit var chartSignal: ImPlotSignalChartView
    private val gson = Gson()

    private val telemetryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                if (intent?.action == TelephonyBackgroundService.ACTION_TELEMETRY_UPDATE) {
                    val json = intent.getStringExtra(TelephonyBackgroundService.EXTRA_TELEMETRY_JSON)
                    Log.d(TAG, "Received telemetry broadcast: ${json?.take(100)}...")
                    
                    json?.let {
                        try {
                            val data = gson.fromJson(it, TelephonyData::class.java)
                            Log.d(TAG, "Parsed telemetry: ${data.cellInfo.size} cells, timestamp=${data.timestamp}")
                            
                            // Update chart
                            try {
                                chartSignal.updateFromTelephonyData(data)
                                Log.d(TAG, "Chart updated successfully")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to update chart: ${e.message}", e)
                            }
                            
                            // Update summary text
                            tvCellInfo.post {
                                tvCellInfo.text = "✓ Sample: ${data.timestamp} | ${data.cellInfo.size} cells | " +
                                    "Lat: ${String.format("%.4f", data.location?.latitude ?: 0.0)}"
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to decode telemetry JSON: ${e.message}", e)
                            tvCellInfo.post {
                                tvCellInfo.text = "Error parsing telemetry: ${e.message}"
                            }
                        }
                    } ?: run {
                        Log.w(TAG, "Received null JSON in telemetry broadcast")
                        tvCellInfo.post {
                            tvCellInfo.text = "Error: No JSON data in broadcast"
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
        setContentView(R.layout.activity_telephony)

        initializeViews()
        initializeZmqViews()

        checkPermissions()
    }

    private fun initializeViews() {
        tvCellInfo = findViewById(R.id.tvCellInfo)
        btnServiceToggle = findViewById(R.id.btnServiceToggle)
        chartSignal = findViewById(R.id.chartSignal)

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
        etZmqHost = findViewById(R.id.etZmqHost)
        etZmqPort = findViewById(R.id.etZmqPort)
        btnZmqToggle = findViewById(R.id.btnZmqToggle)
        btnZmqUpdate = findViewById(R.id.btnZmqUpdate)
        btnShowSent = findViewById(R.id.btnShowSent)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        etZmqHost.setText(prefs.getString(PREF_HOST, "192.168.62.19")) // Default to a common LAN IP
        etZmqPort.setText("2222")

        if (prefs.getBoolean(PREF_SEND_ENABLED, false)) {
            btnZmqToggle.text = "Disable Send"
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
    }

    private fun startTelephonyService(prefs: android.content.SharedPreferences) {
        val intent = Intent(this, TelephonyBackgroundService::class.java)
        intent.action = TelephonyBackgroundService.ACTION_START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
        with(prefs.edit()) {
            putBoolean(PREF_SERVICE_RUNNING, true)
            apply()
        }
        updateServiceToggleButton(prefs)
        Toast.makeText(this, "Background service started", Toast.LENGTH_SHORT).show()
    }

    private fun stopTelephonyService(prefs: android.content.SharedPreferences) {
        val intent = Intent(this, TelephonyBackgroundService::class.java).apply {
            action = TelephonyBackgroundService.ACTION_STOP
        }
        startService(intent)
        with(prefs.edit()) {
            putBoolean(PREF_SERVICE_RUNNING, false)
            apply()
        }
        updateServiceToggleButton(prefs)
        Toast.makeText(this, "Background service stopped", Toast.LENGTH_SHORT).show()
    }

    private fun sendZmqCommand(enable: Boolean, host: String, port: Int) {
        val intent = Intent(this, TelephonyBackgroundService::class.java).apply {
            action = if (enable) TelephonyBackgroundService.ACTION_ENABLE_ZMQ else TelephonyBackgroundService.ACTION_DISABLE_ZMQ
            putExtra(TelephonyBackgroundService.EXTRA_ZMQ_HOST, host)
            putExtra(TelephonyBackgroundService.EXTRA_ZMQ_PORT, port)
        }
        startService(intent)
    }

    private fun checkPermissions() {
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
                // READ_PHONE_NUMBERS might not be available on all devices, don't fail
                try {
                    needed.add(Manifest.permission.READ_PHONE_NUMBERS)
                } catch (e: Exception) {
                    Log.w(TAG, "READ_PHONE_NUMBERS not available on this device")
                }
            }
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), REQUEST_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "Required permissions not granted: ${permissions.joinToString()}")
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
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
        } catch (e: Exception) {
            Log.e(TAG, "Error registering receiver: ${e.message}", e)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            updateServiceToggleButton(prefs)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(telemetryReceiver)
    }
}

