package com.example.calc.controllers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.example.calc.data.model.LocationData
import com.example.calc.data.repository.LocationRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationController(
    private val context: Context,
    private val mainLooper: Looper
) : LocationListener {

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val locationRepository = LocationRepository(context)
    private val handlerThread = HandlerThread("LocationThread").apply { start() }
    private val backgroundHandler = Handler(handlerThread.looper)
    private val mainHandler = Handler(mainLooper)
    private val listeners = mutableListOf<LocationListener>()

    interface LocationListener {
        fun onLocationUpdated(locationData: LocationData)
        fun onLocationStatusChanged(status: String)
        fun onLocationError(message: String)
    }

    fun addListener(listener: LocationListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: LocationListener) {
        listeners.remove(listener)
    }

    fun startLocationUpdates() {
        backgroundHandler.post {
            if (!hasLocationPermission()) {
                mainHandler.post { listeners.forEach { it.onLocationError("Location permission not granted") } }
                return@post
            }

            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000L, 1f, this, handlerThread.looper
                )
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 1000L, 1f, this, handlerThread.looper
                )
                mainHandler.post { listeners.forEach { it.onLocationStatusChanged("Location updates started") } }
            } catch (ex: SecurityException) {
                mainHandler.post { listeners.forEach { it.onLocationError("Failed to start location updates: ${ex.message}") } }
            }
        }
    }

    fun stopLocationUpdates() {
        backgroundHandler.post { locationManager.removeUpdates(this) }
        handlerThread.quitSafely()
    }

    override fun onLocationChanged(location: Location) {
        val locationData = LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            time = location.time,
            provider = location.provider ?: "unknown"
        )
        locationRepository.saveLocation(locationData)
        mainHandler.post { listeners.forEach { it.onLocationUpdated(locationData) } }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {
        mainHandler.post { listeners.forEach { it.onLocationStatusChanged("Provider enabled: $provider") } }
    }
    
    override fun onProviderDisabled(provider: String) {
        mainHandler.post { listeners.forEach { it.onLocationStatusChanged("Provider disabled: $provider") } }
    }

    fun formatTime(timestamp: Long): String {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return timeFormat.format(Date(timestamp))
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val LOCATION_PERMISSION_CODE = 100
    }
}
