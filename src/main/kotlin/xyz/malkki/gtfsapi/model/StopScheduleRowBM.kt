package xyz.malkki.gtfsapi.model

import java.time.LocalDate
import java.time.OffsetDateTime

data class StopScheduleRowBM(
    val tripId: String,
    val tripDate: LocalDate,
    val headsign: String?,
    val arrivalTimeScheduled: OffsetDateTime?,
    val departureTimeScheduled: OffsetDateTime?,
    val dropOff: Boolean,
    val pickUp: Boolean,
)
