package com.example.calc.controllers

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.telephony.CellInfo
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.calc.data.model.TelephonyData
import com.example.calc.data.repository.LocationRepository
import com.example.calc.data.repository.TelephonyRepository
import com.example.calc.controllers.utils.ZmqSender
import com.example.calc.controllers.utils.ClientIdUtil
import com.google.gson.Gson

class TelephonyController(private val context: Context) {

    interface TelephonyListener {
        fun onCellInfo(text: String)
        fun onError(message: String)
    }

    private val TAG = "TelephonyController"

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val telephonyRepository = TelephonyRepository(context)
    private val locationRepository = LocationRepository(context)

    private val handlerThread = HandlerThread("TelephonyThread").apply { start() }
    private val backgroundHandler = Handler(handlerThread.looper)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val refreshIntervalMs = 60_000L
    private var isUpdating = false

    private var listenerForRun: TelephonyListener? = null

    private var zmqSender: ZmqSender? = null
    private var zmqEnabled = false
    private val gson = Gson()

    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: TelephonyCallback? = null

    fun fetchOnce(listener: TelephonyListener) {
        backgroundHandler.post {
            val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                mainHandler.post { listener.onError("No location permission") }
                return@post
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

            try {
                val telephonyData = TelephonyData(timestamp = System.currentTimeMillis(), summary = text)
                telephonyRepository.saveTelephony(telephonyData)
                if (zmqEnabled) {
                    val wrapper = mapOf(
                        "client_id" to ClientIdUtil.getClientId(context),
                        "telephony" to telephonyData,
                        "location" to locationRepository.getLastLocation()
                    )
                    val json = gson.toJson(wrapper)
                    zmqSender?.send(json)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save or send telephony data", e)
            }

            mainHandler.post { listener.onCellInfo(text) }
        }
    }

    fun startUpdates(activity: Activity, listener: TelephonyListener) {
        if (isUpdating) return
        listenerForRun = listener
        isUpdating = true

        val refreshRunnable = object : Runnable {
            override fun run() {
                fetchOnce(listener)
                backgroundHandler.postDelayed(this, refreshIntervalMs)
            }
        }
        backgroundHandler.post(refreshRunnable)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CellInfoListener, TelephonyCallback.SignalStrengthsListener {
                    override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>) {
                        mainHandler.post { listener.onCellInfo(cellInfo.joinToString("\n\n") { it.toString() }) }
                    }

                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        fetchOnce(listener)
                    }
                }
                telephonyManager.registerTelephonyCallback(activity.mainExecutor, telephonyCallback!!)
            } else {
                @Suppress("DEPRECATION")
                phoneStateListener = object : PhoneStateListener() {
                    override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>?) {
                        mainHandler.post { listener.onCellInfo(cellInfo?.joinToString("\n\n") { it.toString() } ?: "No cell info available") }
                    }

                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                        fetchOnce(listener)
                    }
                }
                @Suppress("DEPRECATION")
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CELL_INFO or PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to register telephony listeners", t)
            mainHandler.post { listener.onError("Failed to register telephony listeners: ${t.message}") }
        }
    }

    fun stopUpdates() {
        if (!isUpdating) return
        isUpdating = false
        backgroundHandler.removeCallbacksAndMessages(null)
        handlerThread.quitSafely()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let { telephonyManager.unregisterTelephonyCallback(it) }
            } else {
                phoneStateListener?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to unregister telephony listeners", t)
        }

        disableZmq()
    }

    fun enableZmq(host: String, port: Int) {
        backgroundHandler.post {
            try {
                val endpoint = "tcp://$host:$port"
                zmqSender = ZmqSender(context, endpoint)
                zmqSender?.start()
                zmqEnabled = true
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to enable ZMQ: ${t.message}")
                zmqEnabled = false
            }
        }
    }

    fun disableZmq() {
        backgroundHandler.post {
            zmqEnabled = false
            try {
                zmqSender?.stop()
                zmqSender = null
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to disable ZMQ: ${t.message}")
            }
        }
    }
}
