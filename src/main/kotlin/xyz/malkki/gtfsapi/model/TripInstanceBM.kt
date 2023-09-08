package xyz.malkki.gtfsapi.model

import java.time.LocalDate

data class TripInstanceBM(
    val tripId: String,
    val date: LocalDate,
    val routeId: String,
    val headsign: String?
) : Comparable<TripInstanceBM> {
    override fun compareTo(other: TripInstanceBM): Int {
        val byDate = date.compareTo(other.date)

        return if (byDate == 0) {
            tripId.compareTo(other.tripId)
        } else {
            byDate
        }
    }
}