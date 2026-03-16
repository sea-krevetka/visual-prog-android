package com.example.calc.ui

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.calc.R
import com.example.calc.controllers.LocationController
import com.example.calc.controllers.utils.LocationTracker
import com.example.calc.data.model.LocationData

class LocationActivity : AppCompatActivity(), LocationController.LocationListener {

    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvAltitude: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvStatus: TextView

    private lateinit var locationController: LocationController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        initializeViews()
        locationController = LocationTracker.getInstance(this)
    }

    private fun initializeViews() {
        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        tvAltitude = findViewById(R.id.tvAltitude)
        tvTime = findViewById(R.id.tvTime)
        tvStatus = findViewById(R.id.tvStatus)
    }

    override fun onResume() {
        super.onResume()
        locationController.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        locationController.removeListener(this)
    }

    override fun onLocationUpdated(locationData: LocationData) {
        tvLatitude.text = "Latitude: ${locationData.latitude}"
        tvLongitude.text = "Longitude: ${locationData.longitude}"
        tvAltitude.text = "Altitude: ${locationData.altitude}m"
        tvTime.text = "Time: ${locationController.formatTime(locationData.time)}"
        tvStatus.text = "Location updated: ${locationController.formatTime(System.currentTimeMillis())}"
    }

    override fun onLocationStatusChanged(status: String) {
        tvStatus.text = status
    }

    override fun onLocationError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
