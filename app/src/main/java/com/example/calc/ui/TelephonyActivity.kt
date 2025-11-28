package com.example.calc.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.calc.controllers.TelephonyController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.calc.R

class TelephonyActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_PERMISSION_CODE = 1001
    }

    private val TAG = "TelephonyActivity"

    private lateinit var tvCellInfo: TextView
    private lateinit var etZmqHost: EditText
    private lateinit var etZmqPort: EditText
    private lateinit var btnZmqToggle: Button
    private lateinit var btnShowSent: Button
    private lateinit var telephonyController: TelephonyController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telephony)

        initializeViews()
        telephonyController = TelephonyController(this)
        initializeZmqViews()

        checkPermissions()
    }

    private fun initializeViews() {
        tvCellInfo = findViewById(R.id.tvCellInfo)
    }

    private fun initializeZmqViews() {
        etZmqHost = findViewById(R.id.etZmqHost)
        etZmqPort = findViewById(R.id.etZmqPort)
        btnZmqToggle = findViewById(R.id.btnZmqToggle)

        etZmqHost.setText("127.0.0.1")
        etZmqPort.setText("2222")

        btnZmqToggle.setOnClickListener {
            val enabled = btnZmqToggle.text.toString().contains("Enable", true).not()
            if (enabled.not()) {
                // Currently disabled; enable send
                val host = etZmqHost.text.toString().ifBlank { "127.0.0.1" }
                val port = etZmqPort.text.toString().toIntOrNull() ?: 2222
                try {
                    telephonyController.enableZmq(host, port)
                    btnZmqToggle.text = "Disable Send"
                } catch (t: Throwable) {
                    Log.w(TAG, "Failed to enable ZMQ: ${t.message}")
                    Toast.makeText(this, "Failed to enable ZMQ", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Disable send
                telephonyController.disableZmq()
                btnZmqToggle.text = "Enable Send"
            }
        }
        btnShowSent = findViewById(R.id.btnShowSent)
        btnShowSent.setOnClickListener {
            startActivity(android.content.Intent(this, SentSnapshotsActivity::class.java))
        }
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

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), REQUEST_PERMISSION_CODE)
        } else {

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                if (!isFinishing) startControllerUpdates()
            } else {
                Log.d(TAG, "Required permissions not granted: ${permissions.joinToString()}")
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchCellInfo() {
        telephonyController.fetchOnce(object : TelephonyController.TelephonyListener {
            override fun onCellInfo(text: String) {
                runOnUiThread { tvCellInfo.text = text }
                Log.d(TAG, text)
            }

            override fun onError(message: String) {
                runOnUiThread { tvCellInfo.text = message }
                Log.w(TAG, message)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startControllerUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        stopControllerUpdates()
    }

    private fun startControllerUpdates() {
        telephonyController.startUpdates(this, object : TelephonyController.TelephonyListener {
            override fun onCellInfo(text: String) {
                runOnUiThread { tvCellInfo.text = text }
                Log.d(TAG, text)
            }

            override fun onError(message: String) {
                runOnUiThread { tvCellInfo.text = message }
                Log.w(TAG, message)
            }
        })
    }

    private fun stopControllerUpdates() {
        telephonyController.stopUpdates()
    }
}
