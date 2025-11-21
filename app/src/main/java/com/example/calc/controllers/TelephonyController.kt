package com.example.calc.controller

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.CellInfo
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

class TelephonyController(private val context: Context) {

    interface TelephonyListener {
        fun onCellInfo(text: String)
        fun onError(message: String)
    }

    private val TAG = "TelephonyController"

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private val handler = Handler(Looper.getMainLooper())
    private val refreshIntervalMs = 60_000L
    private var isUpdating = false

    private val refreshRunnable = object : Runnable {
        override fun run() {
            fetchOnce(listenerForRun)
            handler.postDelayed(this, refreshIntervalMs)
        }
    }

    private var listenerForRun: TelephonyListener? = null

    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: TelephonyCallback? = null

    fun fetchOnce(listener: TelephonyListener) {
        val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasCoarse && !hasFine) {
            listener.onError("No location permission")
            return
        }

        val cellInfoList: List<CellInfo>? = try {
            telephonyManager.allCellInfo
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException reading cell info", e)
            null
        }

        val text = if (cellInfoList.isNullOrEmpty()) {
            "No cell info available"
        } else {
            cellInfoList.joinToString(separator = "\n\n") { it.toString() }
        }

        listener.onCellInfo(text)
    }

    fun startUpdates(activity: Activity, listener: TelephonyListener) {
        if (isUpdating) return
        listenerForRun = listener
        isUpdating = true

        fetchOnce(listener)
        handler.postDelayed(refreshRunnable, refreshIntervalMs)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CellInfoListener, TelephonyCallback.SignalStrengthsListener {
                    override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>?) {
                        handler.post { listener.onCellInfo(cellInfo?.joinToString("\n\n") { it.toString() } ?: "No cell info available") }
                    }

                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                        handler.post { fetchOnce(listener) }
                    }
                }
                telephonyManager.registerTelephonyCallback(activity.mainExecutor, telephonyCallback!!)
            } else {
                phoneStateListener = object : PhoneStateListener() {
                    override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>?) {
                        handler.post { listener.onCellInfo(cellInfo?.joinToString("\n\n") { it.toString() } ?: "No cell info available") }
                    }

                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                        handler.post { fetchOnce(listener) }
                    }
                }
                telephonyManager.listen(phoneStateListener!!, PhoneStateListener.LISTEN_CELL_INFO or PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to register telephony listeners", t)
            listener.onError("Failed to register telephony listeners: ${t.message}")
        }
    }

    fun stopUpdates() {
        if (!isUpdating) return
        isUpdating = false
        handler.removeCallbacks(refreshRunnable)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let { telephonyManager.unregisterTelephonyCallback(it) }
                telephonyCallback = null
            } else {
                phoneStateListener?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
                phoneStateListener = null
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to unregister telephony listeners", t)
        }

        listenerForRun = null
    }
}
