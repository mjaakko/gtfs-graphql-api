package xyz.malkki.gtfsapi.model

import java.time.ZoneId

data class StopBM(
    val stopId: String,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val timezone: ZoneId?
)
