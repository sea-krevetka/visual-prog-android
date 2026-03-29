package com.example.calc.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.calc.R
import com.example.calc.controllers.LocationController
import com.example.calc.controllers.TelephonyController
import com.example.calc.data.model.TelephonyData
import com.google.gson.Gson

class TelephonyBackgroundService : Service() {

    companion object {
        const val ACTION_START = "com.example.calc.action.START_TELEPHONY_SERVICE"
        const val ACTION_STOP = "com.example.calc.action.STOP_TELEPHONY_SERVICE"
        const val ACTION_ENABLE_ZMQ = "com.example.calc.action.ENABLE_ZMQ"
        const val ACTION_DISABLE_ZMQ = "com.example.calc.action.DISABLE_ZMQ"
        const val EXTRA_ZMQ_HOST = "com.example.calc.extra.ZMQ_HOST"
        const val EXTRA_ZMQ_PORT = "com.example.calc.extra.ZMQ_PORT"

        const val ACTION_TELEMETRY_UPDATE = "com.example.calc.action.TELEMETRY_UPDATE"
        const val EXTRA_TELEMETRY_JSON = "com.example.calc.extra.TELEMETRY_JSON"

        const val NOTIFICATION_CHANNEL_ID = "telephony_service_channel"
        const val NOTIFICATION_ID = 1001
    }

    private val TAG = "TelephonyBackgroundService"
    private lateinit var telephonyController: TelephonyController
    private lateinit var locationController: LocationController
    private val gson = Gson()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TelephonyBackgroundService.onCreate()")

        telephonyController = TelephonyController(this)
        locationController = LocationController(this, mainLooper)

        try {
            locationController.startLocationUpdates()
            Log.d(TAG, "Location updates started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location updates: ${e.message}", e)
        }

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "TelephonyBackgroundService.onStartCommand() action=${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "ACTION_START received, starting foreground service and data collection")
                startForeground(NOTIFICATION_ID, buildNotification("Telephony monitoring is running"))
                startTelemetryCollection()
            }
            ACTION_STOP -> {
                Log.d(TAG, "ACTION_STOP received, stopping service")
                stopForeground(true)
                stopSelf()
            }
            ACTION_ENABLE_ZMQ -> {
                val host = intent.getStringExtra(EXTRA_ZMQ_HOST) ?: "127.0.0.1"
                val port = intent.getIntExtra(EXTRA_ZMQ_PORT, 2222)
                Log.d(TAG, "ACTION_ENABLE_ZMQ: $host:$port")
                telephonyController.enableZmq(host, port)
            }
            ACTION_DISABLE_ZMQ -> {
                Log.d(TAG, "ACTION_DISABLE_ZMQ received")
                telephonyController.disableZmq()
            }
            else -> {
                Log.d(TAG, "Unknown action, starting foreground service with default state")
                startForeground(NOTIFICATION_ID, buildNotification("Telephony monitoring is ready"))
            }
        }
        return START_STICKY
    }

    private fun startTelemetryCollection() {
        telephonyController.startUpdates(object : TelephonyController.TelephonyListener {
            override fun onCellInfo(text: String) {
                Log.d(TAG, text)
            }

            override fun onCellData(data: TelephonyData) {
                broadcastTelemetry(data)
            }

            override fun onError(message: String) {
                Log.w(TAG, message)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun broadcastTelemetry(data: TelephonyData) {
        val intent = Intent(ACTION_TELEMETRY_UPDATE).apply {
            putExtra(EXTRA_TELEMETRY_JSON, gson.toJson(data))
        }
        sendBroadcast(intent)
    }

    private fun buildNotification(content: String): Notification {
        val pendingIntent = Intent(this, TelephonyActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Telephony Background Service")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Telephony Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for telephony background data collection"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "TelephonyBackgroundService.onDestroy()")
        try {
            telephonyController.stopUpdates()
            Log.d(TAG, "Telephony controller stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping telephony controller: ${e.message}", e)
        }
        try {
            locationController.stopLocationUpdates()
            Log.d(TAG, "Location updates stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates: ${e.message}", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
