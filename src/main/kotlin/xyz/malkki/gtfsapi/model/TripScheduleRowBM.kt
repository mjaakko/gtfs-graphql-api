package xyz.malkki.gtfsapi.model

import java.time.OffsetDateTime

data class TripScheduleRowBM(
    val sequenceNumber: Int,
    val stopId: String,
    val headsign: String?,
    val arrivalTimeScheduled: OffsetDateTime?,
    val departureTimeScheduled: OffsetDateTime?,
    val dropOff: Boolean,
    val pickUp: Boolean
)
