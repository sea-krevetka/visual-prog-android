package com.example.calc.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.calc.R
import com.example.calc.controllers.utils.LocationTracker

class MainActivity : AppCompatActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnGoToCalculator).setOnClickListener {
            startActivity(Intent(this, CalculatorActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToPlayer).setOnClickListener {
            startActivity(Intent(this, MediaPlayerActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToLocation).setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToTelephony).setOnClickListener {
            startActivity(Intent(this, TelephonyActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToZMQ).setOnClickListener {
            startActivity(Intent(this, ZMQActivity::class.java))
        }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (ActivityCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, permissions[1]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            startLocationTracking()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startLocationTracking()
            }
        }
    }

    private fun startLocationTracking() {
        LocationTracker.getInstance(this).startLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocationTracker.getInstance(this).stopLocationUpdates()
    }
}
