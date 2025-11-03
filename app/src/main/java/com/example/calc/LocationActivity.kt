package com.example.calc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvAltitude: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvStatus: TextView

    private val gson = Gson()
    private val locationFile = "locations.json"

    companion object {
        private const val LOCATION_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        initializeViews()
        checkLocationPermissions()
    }

    private fun initializeViews() {
        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        tvAltitude = findViewById(R.id.tvAltitude)
        tvTime = findViewById(R.id.tvTime)
        tvStatus = findViewById(R.id.tvStatus)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private fun checkLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_CODE)
        } else {
            startLocationUpdates()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                getLastKnownLocation()
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Request location updates
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L, // 1 second
            1f, // 1 meter
            this
        )

        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            1000L,
            1f,
            this
        )

        getLastKnownLocation()
        tvStatus.text = "Location updates started"
    }

    private fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        lastKnownLocation?.let {
            updateLocationInfo(it)
        } ?: run {
            tvStatus.text = "No last known location"
        }
    }

    override fun onLocationChanged(location: Location) {
        updateLocationInfo(location)
        saveLocationToFile(location)
    }

    private fun updateLocationInfo(location: Location) {
        tvLatitude.text = "Latitude: ${location.latitude}"
        tvLongitude.text = "Longitude: ${location.longitude}"
        tvAltitude.text = "Altitude: ${location.altitude}m"
        
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        tvTime.text = "Time: ${timeFormat.format(Date(location.time))}"
        
        tvStatus.text = "Location updated: ${timeFormat.format(Date())}"
    }

    private fun saveLocationToFile(location: Location) {
        val locationData = LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            time = location.time,
            provider = location.provider
        )

        val jsonString = gson.toJson(locationData)
        
        try {
            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), locationFile)
            FileOutputStream(file, true).use { fos ->
                fos.write("$jsonString\n".toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        locationManager.removeUpdates(this)
    }

    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double,
        val time: Long,
        val provider: String
    )
}