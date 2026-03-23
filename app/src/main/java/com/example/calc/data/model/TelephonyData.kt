package com.example.calc.data.model

import com.example.calc.data.model.LocationData

data class AppTrafficData(
    val packageName: String,
    val uid: Int,
    val rxBytes: Long,
    val txBytes: Long,
    val totalBytes: Long
)

data class NetworkTrafficData(
    val totalRxBytes: Long,
    val totalTxBytes: Long,
    val mobileRxBytes: Long,
    val mobileTxBytes: Long,
    val topApps: List<AppTrafficData>
)

data class CellInfoData(
    val type: String,
    val identity: Map<String, Any?>,
    val signal: Map<String, Any?>
)

data class TelephonyData(
    val timestamp: Long,
    val cellInfo: List<CellInfoData>,
    val location: LocationData?,
    val traffic: NetworkTrafficData?
)
