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
            val locationPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val readPhoneStatePermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

            if (!locationPermission || !readPhoneStatePermission) {
                mainHandler.post { listener.onError("Required permissions not granted") }
                return@post
            }

            val cellInfoList: List<CellInfo>? = try {
                telephonyManager.allCellInfo
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException reading cell info", e)
                null
            }

            val parsedCellInfo = parseCellInfoList(cellInfoList)
            val latestLocation = locationRepository.getLastLocation()
            val trafficData = collectNetworkTrafficData()

            val telephonyData = TelephonyData(
                timestamp = System.currentTimeMillis(),
                cellInfo = parsedCellInfo,
                location = latestLocation,
                traffic = trafficData
            )

            try {
                telephonyRepository.saveTelephony(telephonyData)
                if (zmqEnabled) {
                    val wrapper = mapOf(
                        "client_id" to ClientIdUtil.getClientId(context),
                        "telephony" to telephonyData
                    )
                    val json = gson.toJson(wrapper)
                    zmqSender?.send(json)
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
        }
    }

    private fun parseCellInfoList(cellInfoList: List<CellInfo>?): List<CellInfoData> {
        if (cellInfoList.isNullOrEmpty()) return emptyList()
        return cellInfoList.mapNotNull { cellInfo ->
            when (cellInfo) {
                is CellInfoLte -> {
                    val identity = cellInfo.cellIdentity
                    val signal = cellInfo.cellSignalStrength
                    CellInfoData(
                        type = "LTE",
                        identity = mapOf(
                            "band" to identity.band,
                            "cellIdentity" to identity.ci,
                            "earfcn" to identity.earfcn,
                            "mcc" to identity.mccString,
                            "mnc" to identity.mncString,
                            "pci" to identity.pci,
                            "tac" to identity.tac
                        ),
                        signal = mapOf(
                            "asuLevel" to signal.asuLevel,
                            "cqi" to signal.cqi,
                            "rsrp" to signal.rsrp,
                            "rsrq" to signal.rsrq,
                            "rssi" to signal.rssi,
                            "rssnr" to signal.rssnr,
                            "timingAdvance" to signal.timingAdvance
                        )
                    )
                }
                is CellInfoGsm -> {
                    val identity = cellInfo.cellIdentity
                    val signal = cellInfo.cellSignalStrength
                    CellInfoData(
                        type = "GSM",
                        identity = mapOf(
                            "cellIdentity" to identity.cid,
                            "bsic" to identity.bsic,
                            "arfcn" to identity.arfcn,
                            "lac" to identity.lac,
                            "mcc" to identity.mccString,
                            "mnc" to identity.mncString,
                            "psc" to identity.psc
                        ),
                        signal = mapOf(
                            "dbm" to signal.dbm,
                            "rssi" to signal.asuLevel,
                            "timingAdvance" to signal.timingAdvance
                        )
                    )
                }
                is CellInfoNr -> {
                    val identity = cellInfo.cellIdentity
                    val signal = cellInfo.cellSignalStrength
                    CellInfoData(
                        type = "NR",
                        identity = mapOf(
                            "band" to identity.bandList?.firstOrNull(),
                            "nci" to (identity.nci.takeIf { it >= 0 }),
                            "pci" to identity.pci,
                            "nrArfcn" to identity.nrarfcn,
                            "tac" to identity.tac,
                            "mcc" to identity.mccString,
                            "mnc" to identity.mncString
                        ),
                        signal = mapOf(
                            "ssRsrp" to signal.ssRsrp,
                            "ssRsrq" to signal.ssRsrq,
                            "ssSinr" to signal.ssSinr,
                            "timingAdvance" to signal.timingAdvance
                        )
                    )
                }
                else -> null
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
                        val text = if (cellInfo.isEmpty()) "No cell info available" else gson.toJson(parseCellInfoList(cellInfo))
                        mainHandler.post { listener.onCellInfo(text) }
                    }

                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        fetchOnce(listener)
                    }
                }
                telephonyManager.registerTelephonyCallback(callbackExecutor, telephonyCallback!!)
            } else {
                @Suppress("DEPRECATION")
                phoneStateListener = object : PhoneStateListener() {
                    override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>?) {
                        val text = if (cellInfo.isNullOrEmpty()) "No cell info available" else gson.toJson(parseCellInfoList(cellInfo))
                        mainHandler.post { listener.onCellInfo(text) }
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

