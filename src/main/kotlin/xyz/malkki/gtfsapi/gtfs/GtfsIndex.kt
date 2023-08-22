package xyz.malkki.gtfsapi.gtfs

import com.eatthepath.jvptree.VPTree
import com.github.benmanes.caffeine.cache.Caffeine
import xyz.malkki.gtfs.model.Agency
import xyz.malkki.gtfs.model.Calendar
import xyz.malkki.gtfs.model.CalendarDate
import xyz.malkki.gtfs.model.Route
import xyz.malkki.gtfs.model.Shape
import xyz.malkki.gtfs.model.Stop
import xyz.malkki.gtfs.model.StopTime
import xyz.malkki.gtfs.model.Trip
import xyz.malkki.gtfsapi.common.LatLng
import xyz.malkki.gtfsapi.extensions.location
import xyz.malkki.gtfsapi.utils.encodePolyline

class GtfsIndex(
    agencies: List<Agency>,
    routes: List<Route>,
    trips: List<Trip>,
    stops: List<Stop>,
    stopTimes: List<StopTime>,
    calendars: List<Calendar>,
    calendarDates: List<CalendarDate>,
    shapes: List<Shape>
) {
    val agenciesById = agencies.associateBy { it.agencyId }
    val routesById = routes.associateBy { it.routeId }
    val tripsById = trips.associateBy { it.tripId }
    val tripsByRouteId = trips.groupBy { it.routeId }
    val stopsById = stops.associateBy { it.stopId }
    val stopTimesByTripId = stopTimes.groupBy { it.tripId }.mapValues { it.value.toSortedSet() }
    val stopTimesByStopId = stopTimes.groupBy { it.stopId }
    val shapesById = shapes.groupBy { it.shapeId }.mapValues { it.value.toSortedSet() }

    val serviceDates = ServiceDates(calendars, calendarDates)

    private val stopsVpTree = VPTree<Geopoint, StopWrapper>(
        { first, second -> first.distanceTo(second) },
        stops.filter { it.location != null }.map { StopWrapper(it) }
    )

    private val polylineCache = Caffeine.newBuilder()
        .maximumWeight(1_000_000)
        .weigher { _: String, value: String -> value.length }
        .build<String, String> { shapeId ->
            encodePolyline(shapeId)
        }

    fun getStopsNearLocation(location: LatLng, maxDistance: Double): List<Stop> {
        return stopsVpTree.getAllWithinDistance(LatLngWrapper(location), maxDistance).map { it.stop }
    }

    private fun encodePolyline(shapeId: String): String? {
        return shapesById[shapeId]?.let { shape ->
            val points = shape.map { it.location }
            encodePolyline(points)
        }
    }

    fun getEncodedPolyline(shapeId: String): String? {
        return polylineCache.get(shapeId)
    }

    private interface Geopoint {
        val lat: Double
        val lon: Double

        fun distanceTo(other: Geopoint): Double
    }

    private data class StopWrapper(val stop: Stop) : Geopoint {
        override val lat = stop.stopLat!!
        override val lon = stop.stopLon!!

        override fun distanceTo(other: Geopoint): Double = LatLng(lat, lon).distanceTo(LatLng(lat, lon))
    }

    private data class LatLngWrapper(val latLng: LatLng) : Geopoint {
        override val lat = latLng.latitude
        override val lon = latLng.longitude

        override fun distanceTo(other: Geopoint): Double = latLng.distanceTo(LatLng(other.lat, other.lon))
    }
}