package com.example.calc.controllers.utils

import android.content.Context
import android.os.Looper
import com.example.calc.controllers.LocationController

object LocationTracker {
    private var instance: LocationController? = null

    fun getInstance(context: Context): LocationController {
        return instance ?: synchronized(this) {
            instance ?: LocationController(context.applicationContext, Looper.getMainLooper()).also {
                instance = it
            }
        }
    }
}
