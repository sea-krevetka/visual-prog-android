package com.example.calc.data.model

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val time: Long,
    val provider: String
)