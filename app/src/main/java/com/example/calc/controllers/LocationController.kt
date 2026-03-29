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
import android.util.Log
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

    private val TAG = "LocationController"
    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val locationRepository = LocationRepository(context)
    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val mainHandler = Handler(mainLooper)
    private val listeners = mutableListOf<LocationListener>()
    private var isRunning = false

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
        if (isRunning) {
            Log.w(TAG, "Location updates already running")
            return
        }
        
        // Create new HandlerThread each time (old one is quit)
        if (handlerThread == null || !handlerThread!!.isAlive) {
            handlerThread = HandlerThread("LocationThread").apply { start() }
            backgroundHandler = Handler(handlerThread!!.looper)
        }
        
        backgroundHandler?.post {
            Log.d(TAG, "startLocationUpdates called")
            if (!hasLocationPermission()) {
                Log.e(TAG, "Location permission not granted")
                mainHandler.post { listeners.forEach { it.onLocationError("Location permission not granted") } }
                return@post
            }

            try {
                Log.d(TAG, "Requesting GPS_PROVIDER updates (1000ms, 1m threshold)")
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000L, 1f, this@LocationController, handlerThread!!.looper
                )
                Log.d(TAG, "Requesting NETWORK_PROVIDER updates (1000ms, 1m threshold)")
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 1000L, 1f, this@LocationController, handlerThread!!.looper
                )
                Log.d(TAG, "Location updates started successfully")
                isRunning = true
                mainHandler.post { listeners.forEach { it.onLocationStatusChanged("Location updates started") } }
            } catch (ex: SecurityException) {
                Log.e(TAG, "Failed to start location updates: ${ex.message}", ex)
                mainHandler.post { listeners.forEach { it.onLocationError("Failed to start location updates: ${ex.message}") } }
            }
        }
    }

    fun stopLocationUpdates() {
        if (!isRunning) return
        isRunning = false
        Log.d(TAG, "Stopping location updates")
        try {
            locationManager.removeUpdates(this)
            Log.d(TAG, "Location manager updates removed")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing location updates: ${e.message}")
        }
        // Don't quit the handler thread here, just let it exist for reuse
    }

    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "Location update received: lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}m, provider=${location.provider}")
        val locationData = LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            time = location.time,
            accuracy = location.accuracy,
            provider = location.provider ?: "unknown"
        )
        Log.d(TAG, "Saving location data: $locationData")
        locationRepository.saveLocation(locationData)
        mainHandler.post { listeners.forEach { it.onLocationUpdated(locationData) } }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {
        Log.d(TAG, "Location provider enabled: $provider")
        mainHandler.post { listeners.forEach { it.onLocationStatusChanged("Provider enabled: $provider") } }
    }
    
    override fun onProviderDisabled(provider: String) {
        Log.d(TAG, "Location provider disabled: $provider")
        mainHandler.post { listeners.forEach { it.onLocationStatusChanged("Provider disabled: $provider") } }
    }

    fun formatTime(timestamp: Long): String {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return timeFormat.format(Date(timestamp))
    }

    private fun hasLocationPermission(): Boolean {
        val hasFine = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Permission check - FINE: $hasFine, COARSE: $hasCoarse")
        return hasFine || hasCoarse
    }

    companion object {
        const val LOCATION_PERMISSION_CODE = 100
    }
}
