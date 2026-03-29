package com.example.calc.controllers

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.TrafficStats
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.calc.data.model.AppTrafficData
import com.example.calc.data.model.CellInfoData
import com.example.calc.data.model.LocationData
import com.example.calc.data.model.NetworkTrafficData
import com.example.calc.data.model.TelephonyData
import com.example.calc.data.repository.LocationRepository
import com.example.calc.data.repository.TelephonyRepository
import com.example.calc.controllers.utils.ClientIdUtil
import com.example.calc.controllers.utils.ZmqSender
import com.example.calc.utils.CrashLogger
import com.google.gson.Gson
import kotlin.math.pow
import kotlin.math.sqrt

class TelephonyController(private val context: Context) {

    interface TelephonyListener {
        fun onCellInfo(text: String)
        fun onCellData(data: TelephonyData)
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
            try {
                Log.d(TAG, "fetchOnce called")
                val locationPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val readPhoneStatePermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

                Log.d(TAG, "Permissions - location: $locationPermission, phone: $readPhoneStatePermission")
                
                if (!locationPermission || !readPhoneStatePermission) {
                    Log.w(TAG, "Missing permissions")
                    mainHandler.post { listener.onError("Required permissions not granted") }
                    return@post
                }

                val cellInfoList: List<CellInfo>? = try {
                    val cells = telephonyManager.allCellInfo
                    Log.d(TAG, "Got cellInfoList: ${cells?.size} items")
                    cells
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException reading cell info", e)
                    CrashLogger.logException(TAG, "SecurityException reading cell info", e)
                    null
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error reading cell info: ${e.message}", e)
                    CrashLogger.logException(TAG, "Unexpected error reading cell info", e)
                    null
                }

                val parsedCellInfo = parseCellInfoList(cellInfoList)
                Log.d(TAG, "Parsed cell info: ${parsedCellInfo.size} cells")
                
                val latestLocation = locationRepository.getLastLocation()
                Log.d(TAG, "Got location: $latestLocation")
                
                val trafficData = collectNetworkTrafficData()
                Log.d(TAG, "Got traffic: $trafficData")

                val telephonyData = TelephonyData(
                    timestamp = System.currentTimeMillis(),
                    cellInfo = parsedCellInfo,
                    location = latestLocation,
                    traffic = trafficData
                )

                Log.d(TAG, "Collected data: ${parsedCellInfo.size} cells, location=${latestLocation?.latitude}, traffic=${trafficData}")

                try {
                    telephonyRepository.saveTelephony(telephonyData)
                    Log.d(TAG, "Data saved successfully")
                    if (zmqEnabled) {
                        val wrapper = mapOf(
                            "client_id" to ClientIdUtil.getClientId(context),
                            "telephony" to telephonyData
                        )
                        val json = gson.toJson(wrapper)
                        zmqSender?.send(json)
                        Log.d(TAG, "Data sent via ZMQ")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to save or send telephony data", e)
                }

                val text = if (parsedCellInfo.isEmpty()) {
                    "No cell info available"
                } else {
                    gson.toJson(telephonyData)
                }

                mainHandler.post {
                    listener.onCellData(telephonyData)
                    listener.onCellInfo(text)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error in fetchOnce: ${e.message}", e)
                CrashLogger.logException(TAG, "Fatal error in fetchOnce", e)
                mainHandler.post { listener.onError("Error fetching telephony data: ${e.message}") }
            }
        }
    }

    private fun parseCellInfoList(cellInfoList: List<CellInfo>?): List<CellInfoData> {
        if (cellInfoList.isNullOrEmpty()) return emptyList()
        return cellInfoList.mapNotNull { cellInfo ->
            try {
                when (cellInfo) {
                    is CellInfoLte -> {
                        val identity = cellInfo.cellIdentity as? CellIdentityLte ?: return@mapNotNull null
                        val signal = cellInfo.cellSignalStrength as? CellSignalStrengthLte ?: return@mapNotNull null
                        
                        val identityMap = mutableMapOf<String, Any?>()
                        identityMap["cellIdentity"] = identity.ci
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            identityMap["earfcn"] = identity.earfcn
                            identityMap["pci"] = identity.pci
                            identityMap["tac"] = identity.tac
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            identityMap["mcc"] = identity.mccString
                            identityMap["mnc"] = identity.mncString
                        }
                        
                        val signalMap = mutableMapOf<String, Any?>()
                        signalMap["asuLevel"] = signal.asuLevel
                        signalMap["rsrp"] = signal.rsrp
                        signalMap["rsrq"] = signal.rsrq
                        signalMap["rssi"] = signal.rssi
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            signalMap["rssnr"] = signal.rssnr
                            signalMap["timingAdvance"] = signal.timingAdvance
                        }
                        
                        CellInfoData(
                            type = "LTE",
                            identity = identityMap,
                            signal = signalMap
                        )
                    }
                    is CellInfoGsm -> {
                        val identity = cellInfo.cellIdentity as? CellIdentityGsm ?: return@mapNotNull null
                        val signal = cellInfo.cellSignalStrength as? CellSignalStrengthGsm ?: return@mapNotNull null
                        
                        val identityMap = mutableMapOf<String, Any?>()
                        identityMap["cellIdentity"] = identity.cid
                        identityMap["lac"] = identity.lac
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            identityMap["mcc"] = identity.mccString
                            identityMap["mnc"] = identity.mncString
                            identityMap["arfcn"] = identity.arfcn
                            identityMap["bsic"] = identity.bsic
                        }
                        
                        val signalMap = mutableMapOf<String, Any?>()
                        signalMap["dbm"] = signal.dbm
                        signalMap["asuLevel"] = signal.asuLevel
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            signalMap["timingAdvance"] = signal.timingAdvance
                        }
                        
                        CellInfoData(
                            type = "GSM",
                            identity = identityMap,
                            signal = signalMap
                        )
                    }
                    is CellInfoNr -> {
                        val identity = cellInfo.cellIdentity as? CellIdentityNr ?: return@mapNotNull null
                        val signal = cellInfo.cellSignalStrength as? CellSignalStrengthNr ?: return@mapNotNull null
                        
                        val identityMap = mutableMapOf<String, Any?>()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                identityMap["nci"] = identity.nci.takeIf { it >= 0 }
                                identityMap["pci"] = identity.pci
                                identityMap["tac"] = identity.tac
                                identityMap["mcc"] = identity.mccString
                                identityMap["mnc"] = identity.mncString
                                // Note: nrArfcn may not be available on all devices
                                try {
                                    val nrArfcnMethod = identity.javaClass.getMethod("getNrArfcn")
                                    identityMap["nrArfcn"] = nrArfcnMethod.invoke(identity)
                                } catch (e: NoSuchMethodException) {
                                    // Property not available on this device
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error reading NR identity fields: ${e.message}")
                            }
                        }
                        
                        val signalMap = mutableMapOf<String, Any?>()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                signalMap["ssRsrp"] = signal.ssRsrp
                                signalMap["ssRsrq"] = signal.ssRsrq
                                signalMap["ssSinr"] = signal.ssSinr
                            } catch (e: Exception) {
                                Log.w(TAG, "Error reading NR signal fields: ${e.message}")
                            }
                        }
                        
                        CellInfoData(
                            type = "NR",
                            identity = identityMap,
                            signal = signalMap
                        )
                    }
                    else -> null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing cell info: ${e.message}", e)
                null
            }
        }
    }

    private fun collectNetworkTrafficData(): NetworkTrafficData? {
        return try {
            val totalRxBytes = TrafficStats.getTotalRxBytes().takeIf { it >= 0 } ?: 0L
            val totalTxBytes = TrafficStats.getTotalTxBytes().takeIf { it >= 0 } ?: 0L
            val mobileRxBytes = TrafficStats.getMobileRxBytes().takeIf { it >= 0 } ?: 0L
            val mobileTxBytes = TrafficStats.getMobileTxBytes().takeIf { it >= 0 } ?: 0L

            val appInfoList = context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            val trafficPerApp = appInfoList.mapNotNull { appInfo ->
                val uid = appInfo.uid
                val rx = TrafficStats.getUidRxBytes(uid).takeIf { it >= 0 } ?: 0L
                val tx = TrafficStats.getUidTxBytes(uid).takeIf { it >= 0 } ?: 0L
                val total = rx + tx
                if (total <= 0L) null else AppTrafficData(packageName = appInfo.packageName, uid = uid, rxBytes = rx, txBytes = tx, totalBytes = total)
            }.sortedByDescending { it.totalBytes }

            val topApps = if (trafficPerApp.isEmpty()) emptyList() else {
                val totals = trafficPerApp.map { it.totalBytes.toDouble() }
                val mean = totals.average()
                val stddev = sqrt(totals.map { (it - mean).pow(2.0) }.average())
                val cutoff = mean + 2 * stddev
                val high = trafficPerApp.filter { it.totalBytes.toDouble() > cutoff }.sortedByDescending { it.totalBytes }
                if (high.isNotEmpty()) high.take(10) else trafficPerApp.take(10)
            }

            NetworkTrafficData(
                totalRxBytes = totalRxBytes,
                totalTxBytes = totalTxBytes,
                mobileRxBytes = mobileRxBytes,
                mobileTxBytes = mobileTxBytes,
                topApps = topApps
            )
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to collect network traffic data: ${t.message}")
            null
        }
    }

    fun startUpdates(listener: TelephonyListener, callbackExecutor: java.util.concurrent.Executor = ContextCompat.getMainExecutor(context)) {
        if (isUpdating) {
            Log.w(TAG, "Already updating, ignoring startUpdates call")
            return
        }
        Log.d(TAG, "Starting telemetry updates")
        listenerForRun = listener
        isUpdating = true

        val refreshRunnable = object : Runnable {
            override fun run() {
                Log.d(TAG, "Running refresh runnable - interval: ${refreshIntervalMs}ms")
                fetchOnce(listener)
                Log.d(TAG, "Scheduling next refresh in ${refreshIntervalMs}ms")
                backgroundHandler.postDelayed(this, refreshIntervalMs)
            }
        }
        Log.d(TAG, "Posting initial refresh runnable")
        backgroundHandler.post(refreshRunnable)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.d(TAG, "OS >= Android 12, using TelephonyCallback")
                telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CellInfoListener, TelephonyCallback.SignalStrengthsListener {
                    override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>) {
                        Log.d(TAG, "onCellInfoChanged called with ${cellInfo.size} cells")
                        val text = if (cellInfo.isEmpty()) "No cell info available" else gson.toJson(parseCellInfoList(cellInfo))
                        mainHandler.post { listener.onCellInfo(text) }
                    }

                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        Log.d(TAG, "onSignalStrengthsChanged called")
                        fetchOnce(listener)
                    }
                }
                Log.d(TAG, "Registering TelephonyCallback")
                telephonyManager.registerTelephonyCallback(callbackExecutor, telephonyCallback!!)
            } else {
                Log.d(TAG, "OS < Android 12, using deprecated PhoneStateListener")
                @Suppress("DEPRECATION")
                phoneStateListener = object : PhoneStateListener() {
                    override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>?) {
                        Log.d(TAG, "onCellInfoChanged (deprecated) called with ${cellInfo?.size ?: 0} cells")
                        val text = if (cellInfo.isNullOrEmpty()) "No cell info available" else gson.toJson(parseCellInfoList(cellInfo))
                        mainHandler.post { listener.onCellInfo(text) }
                    }

                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                        Log.d(TAG, "onSignalStrengthsChanged (deprecated) called")
                        fetchOnce(listener)
                    }
                }
                @Suppress("DEPRECATION")
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CELL_INFO or PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                Log.d(TAG, "PhoneStateListener registered")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to register telephony listeners", t)
            mainHandler.post { listener.onError("Failed to register telephony listeners: ${t.message}") }
        }
    }

    fun startUpdates(activity: Activity, listener: TelephonyListener) {
        startUpdates(listener, activity.mainExecutor)
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
                Log.d(TAG, "ZMQ enabled: $endpoint - starting batch send of saved data")
                
                // Batch send all saved telemetry data
                backgroundHandler.postDelayed({
                    try {
                        zmqSender?.batchSendAllSavedData()
                        Log.d(TAG, "Batch send initiated")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in batch send: ${e.message}", e)
                    }
                }, 500)  // Give connection 500ms to establish
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to enable ZMQ: ${t.message}", t)
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

    fun isZmqEnabled(): Boolean {
        return zmqEnabled
    }

    fun updateZmqEndpoint(host: String, port: Int) {
        backgroundHandler.post {
            if (!zmqEnabled) return@post
            try {
                zmqSender?.stop()
                zmqSender = null
                val endpoint = "tcp://$host:$port"
                zmqSender = ZmqSender(context, endpoint)
                zmqSender?.start()
                Log.d(TAG, "ZMQ endpoint updated to $endpoint")
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to update ZMQ endpoint: ${t.message}")
                zmqEnabled = false
            }
        }
    }
}

