package com.example.calc.controller

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.calc.data.model.LocationData
import com.example.calc.data.repository.LocationRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationController(
    private val context: Context,
    private val listener: LocationListener
) : LocationListener {

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val locationRepository = LocationRepository(context)

    interface LocationListener {
        fun onLocationUpdated(locationData: LocationData)
        fun onLocationStatusChanged(status: String)
        fun onLocationError(message: String)
    }

    fun checkLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            listener.onLocationStatusChanged("Requesting location permissions...")
        } else {
            startLocationUpdates()
        }
    }

    fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            listener.onLocationError("Location permission not granted")
            return
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L,
            1f,
            this
        )

        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            1000L,
            1f,
            this
        )

        getLastKnownLocation()
        listener.onLocationStatusChanged("Location updates started")
    }

    fun stopLocationUpdates() {
        locationManager.removeUpdates(this)
    }

    private fun getLastKnownLocation() {
        if (!hasLocationPermission()) return

        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        lastKnownLocation?.let {
            onLocationChanged(it)
        } ?: run {
            listener.onLocationStatusChanged("No last known location")
        }
    }

    override fun onLocationChanged(location: Location) {
        val locationData = LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            time = location.time,
            provider = location.provider ?: "unknown"
        )
        
        listener.onLocationUpdated(locationData)
        locationRepository.saveLocation(locationData)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {
        listener.onLocationStatusChanged("Provider enabled: $provider")
    }
    
    override fun onProviderDisabled(provider: String) {
        listener.onLocationStatusChanged("Provider disabled: $provider")
    }

    fun onPermissionResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            listener.onLocationError("Location permission denied")
            getLastKnownLocation()
        }
    }

    fun formatTime(timestamp: Long): String {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return timeFormat.format(Date(timestamp))
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val LOCATION_PERMISSION_CODE = 100
    }
}