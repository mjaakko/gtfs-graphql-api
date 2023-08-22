package xyz.malkki.gtfsapi.model

import java.time.LocalDate
import java.time.OffsetDateTime

data class VehiclePositionBM(
    val tripId: String,
    val tripDate: LocalDate,
    val vehicleId: String,
    val vehicleLabel: String?,
    val latitude: Float,
    val longitude: Float,
    val bearing: Float?,
    val speed: Float?,
    val status: String,
    val currentStopSequence: Int?,
    val stopId: String?,
    val timestamp: OffsetDateTime
)
