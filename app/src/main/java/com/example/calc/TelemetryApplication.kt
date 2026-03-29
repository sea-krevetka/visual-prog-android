package com.example.calc

import android.app.Application
import android.util.Log
import com.example.calc.utils.CrashLogger

class TelemetryApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize crash logger as early as possible
        try {
            CrashLogger.init(this)
            Log.d("TelemetryApplication", "CrashLogger initialized")
        } catch (e: Exception) {
            Log.e("TelemetryApplication", "Failed to initialize CrashLogger", e)
        }
    }
}
