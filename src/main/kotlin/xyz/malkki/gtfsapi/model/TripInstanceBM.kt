package xyz.malkki.gtfsapi.model

import java.time.LocalDate

data class TripInstanceBM(
    val id: String,
    val date: LocalDate,
    val routeId: String,
    val headsign: String?
) : Comparable<TripInstanceBM> {
    override fun compareTo(other: TripInstanceBM): Int {
        val byDate = date.compareTo(other.date)

        return if (byDate == 0) {
            id.compareTo(other.id)
        } else {
            byDate
        }
    }
}