package xyz.malkki.gtfsapi

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import xyz.malkki.gtfs.model.Agency
import xyz.malkki.gtfs.model.Stop
import xyz.malkki.gtfs.model.StopTime
import xyz.malkki.gtfs.utils.GtfsTimeUtils
import xyz.malkki.gtfsapi.common.LatLng
import xyz.malkki.gtfsapi.extensions.hasDropOff
import xyz.malkki.gtfsapi.extensions.hasPickUp
import xyz.malkki.gtfsapi.extensions.location
import xyz.malkki.gtfsapi.gtfs.GtfsIndex
import xyz.malkki.gtfsapi.model.AgencyBM
import xyz.malkki.gtfsapi.model.RouteBM
import xyz.malkki.gtfsapi.model.StopBM
import xyz.malkki.gtfsapi.model.StopScheduleRowBM
import xyz.malkki.gtfsapi.model.TripInstanceBM
import xyz.malkki.gtfsapi.model.TripScheduleRowBM
import xyz.malkki.gtfsapi.utils.encodePolyline
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

private val log = KotlinLogging.logger {}

@Service
class GtfsService(@Autowired private val gtfsIndexProvider: GtfsIndexProvider) {
    private lateinit var gtfsIndex: GtfsIndex

    init {
        gtfsIndexProvider.gtfsIndexFlux.subscribe {
            log.info("Updated GTFS index")
            gtfsIndex = it
        }
    }

    fun getStops(): Collection<StopBM> {
        return gtfsIndex.stopsById.values
            .filter { it.locationType == null || it.locationType == Stop.LOCATION_TYPE_STOP }
            .map { it.toBM() }
    }

    fun getRoutes(): List<RouteBM> {
        return gtfsIndex.routesById.values.map {
            RouteBM(
                it.routeId,
                it.routeShortName,
                it.routeLongName,
                it.agencyId
            )
        }
    }

    fun getRoute(routeId: String): RouteBM? {
        return gtfsIndex.routesById[routeId]?.let {
            RouteBM(
                it.routeId,
                it.routeShortName,
                it.routeLongName,
                it.agencyId
            )
        }
    }

    fun getAgencies(): List<AgencyBM> {
        return gtfsIndex.agenciesById.values.map { it.toBM() }
    }

    fun getAgency(agencyId: String): AgencyBM? {
        return gtfsIndex.agenciesById[agencyId]?.toBM()
    }

    fun getTripInstancesForRoute(routeId: String, from: LocalDate?, to: LocalDate?): List<TripInstanceBM> {
        val trips = gtfsIndex.tripsByRouteId[routeId] ?: emptyList()

        return trips.flatMap { trip ->
            val dates = gtfsIndex.serviceDates.getDatesForServiceId(trip.serviceId).let {
                if (from == null && to == null) {
                    it
                } else if (from == null) {
                    it.headSet(to, true)
                } else if (to == null) {
                    it.tailSet(from, true)
                } else {
                    it.subSet(from, true, to, true)
                }
            }

            dates.map {
                TripInstanceBM(
                    trip.tripId,
                    it,
                    trip.routeId,
                    trip.tripHeadsign
                )
            }
        }.sorted()
    }

    fun getTripInstance(tripId: String, date: LocalDate): TripInstanceBM? {
        val trip = gtfsIndex.tripsById[tripId] ?: return null

        if (date !in gtfsIndex.serviceDates.getDatesForServiceId(trip.serviceId)) {
            return null
        }

        return TripInstanceBM(
            trip.tripId,
            date,
            trip.routeId,
            trip.tripHeadsign
        )
    }

    fun getTripId(routeId: String, startTime: Int, date: LocalDate, directionId: Int?): String? {
        return gtfsIndex.getTripId(routeId, startTime, date, directionId)
    }

    fun getStopsNearby(location: LatLng, radius: Double): List<StopBM> {
        return gtfsIndex.getStopsNearLocation(location, radius)
            .filter { it.locationType == null || it.locationType == Stop.LOCATION_TYPE_STOP }
            .map { it.toBM() }
    }

    fun getTripScheduleRows(tripId: String, date: LocalDate): List<TripScheduleRowBM> {
        val agencyTimezone = gtfsIndex.agenciesById[gtfsIndex.routesById[gtfsIndex.tripsById[tripId]!!.routeId]!!.agencyId]!!.agencyTimezone

        return gtfsIndex.stopTimesByTripId[tripId]?.map {
            val stopTimezone = gtfsIndex.stopsById[it.stopId]!!.stopTimezone ?: agencyTimezone

            TripScheduleRowBM(
                it.stopSequence,
                it.stopId,
                it.stopHeadsign,
                it.arrivalTime?.let { time -> getTime(time, date, agencyTimezone, stopTimezone) },
                it.departureTime?.let { time -> getTime(time, date, agencyTimezone, stopTimezone) },
                it.hasDropOff,
                it.hasPickUp
            )
        } ?: emptyList()
    }

    private fun getTime(gtfsTime: Int, date: LocalDate, agencyTimezone: ZoneId, stopTimezone: ZoneId): OffsetDateTime {
        return GtfsTimeUtils.gtfsTimeToZonedDateTime(date, gtfsTime, agencyTimezone)
            .withZoneSameInstant(stopTimezone)
            .toOffsetDateTime()
    }

    fun getStopById(stopId: String): StopBM? {
        return gtfsIndex.stopsById[stopId]?.toBM()
    }

    fun getShapeByTripId(tripId: String): String? {
        return gtfsIndex.tripsById[tripId]?.let { trip ->
            val shape = trip.shapeId?.let { shapeId -> gtfsIndex.getEncodedPolyline(shapeId) }

            return if (shape == null) {
                //If shape is not available, create polyline from stop locations
                val stopLocations = gtfsIndex.stopTimesByTripId[trip.tripId]!!
                    .map { gtfsIndex.stopsById[it.stopId]!!.location!! }

                encodePolyline(stopLocations)
            } else {
                shape
            }
        }
    }

    private fun findEarliestServiceDay(dateTime: ZonedDateTime, zoneId: ZoneId, departureTime: Int): LocalDate {
        var date = dateTime.withZoneSameInstant(zoneId).minusSeconds(departureTime.toLong()).minusDays(1).toLocalDate()

        while (GtfsTimeUtils.gtfsTimeToZonedDateTime(date.plusDays(1), departureTime, zoneId) <= dateTime) {
            date = date.plusDays(1)
        }

        return date
    }

    private fun findLatestServiceDay(dateTime: ZonedDateTime, zoneId: ZoneId, departureTime: Int): LocalDate {
        var date = dateTime.withZoneSameInstant(zoneId).minusSeconds(departureTime.toLong()).plusDays(1).toLocalDate()

        while (GtfsTimeUtils.gtfsTimeToZonedDateTime(date.minusDays(1), departureTime, zoneId) > dateTime) {
            date = date.minusDays(1)
        }

        return date
    }

    private val StopTime.agencyTimezone: ZoneId
        get() = gtfsIndex.agenciesById[gtfsIndex.routesById[gtfsIndex.tripsById[this.tripId]!!.routeId]!!.agencyId]!!.agencyTimezone

    /**
     * Gets schedule rows for the stop
     *
     * @param max Maximum number of rows to return
     * @param includeLastStop Whether to include schedule rows where this stop is the final stop of a trip
     */
    fun getScheduleRowsForStop(
        stopId: String,
        from: ZonedDateTime,
        to: ZonedDateTime,
        max: Int? = null,
        includeLastStop: Boolean = true
    ): List<StopScheduleRowBM> {
        val stop = gtfsIndex.stopsById[stopId] ?: return emptyList()
        val stopTimes = gtfsIndex.stopTimesByStopId[stop.stopId] ?: return emptyList()

        val stopTimezone = stop.stopTimezone

        val stopTimesWithArrival = stopTimes.filter { it.arrivalTime != null }

        if (stopTimesWithArrival.isEmpty()) {
            return emptyList()
        }

        val stopTimeWithMaxArrivalTime = stopTimesWithArrival.maxByOrNull { it.arrivalTime!! }!!
        val stopTimeWithMinArrivalTime = stopTimesWithArrival.minByOrNull { it.arrivalTime!! }!!

        val minServiceDay = findEarliestServiceDay(from, stopTimeWithMaxArrivalTime.agencyTimezone, stopTimeWithMaxArrivalTime.arrivalTime!!)
        val maxServiceDay = findLatestServiceDay(to, stopTimeWithMinArrivalTime.agencyTimezone, stopTimeWithMinArrivalTime.arrivalTime!!)

        val scheduleRows = stopTimes
            .filter { stopTime -> includeLastStop || !stopTime.isLastStop }
            .flatMap { stopTime ->
                val trip = gtfsIndex.tripsById[stopTime.tripId]!!

                gtfsIndex.serviceDates.getDatesForServiceId(trip.serviceId).subSet(minServiceDay, true, maxServiceDay, true).map { date ->
                    StopScheduleRowBM(
                        trip.tripId,
                        date,
                        stopTime.stopHeadsign,
                        stopTime.arrivalTime?.let { arrivalTime ->
                            getTime(arrivalTime, date, stopTime.agencyTimezone, stopTimezone ?: stopTime.agencyTimezone)
                        },
                        stopTime.departureTime?.let { departureTime ->
                            getTime(departureTime, date, stopTime.agencyTimezone, stopTimezone ?: stopTime.agencyTimezone)
                        },
                        stopTime.hasDropOff,
                        stopTime.hasPickUp
                    )
                }
            }
            .filter {
                (it.arrivalTimeScheduled != null
                        && it.arrivalTimeScheduled >= from.toOffsetDateTime()
                        && it.arrivalTimeScheduled <= to.toOffsetDateTime())
                || (it.departureTimeScheduled != null
                        && it.departureTimeScheduled >= from.toOffsetDateTime()
                        && it.departureTimeScheduled <= to.toOffsetDateTime())
            }
            .sortedBy { it.arrivalTimeScheduled }

        return if (max != null) {
            scheduleRows.take(max)
        } else {
            scheduleRows
        }
    }

    /**
     * Whether this StopTime is the last stop of the trip
     */
    private val StopTime.isLastStop: Boolean
        get() = gtfsIndex.stopTimesByTripId[tripId]?.last() == this

    private fun Stop.toBM(): StopBM {
        return StopBM(
            stopId,
            stopName,
            stopLat,
            stopLon,
            stopTimezone
        )
    }

    private fun Agency.toBM(): AgencyBM {
        return AgencyBM(
            agencyId,
            agencyName,
            agencyUrl,
            agencyTimezone.id,
            agencyLang,
            agencyPhone,
            agencyFareUrl,
            agencyEmail
        )
    }
}