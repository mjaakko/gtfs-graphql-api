package xyz.malkki.gtfsapi

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import xyz.malkki.gtfsapi.common.LatLng
import xyz.malkki.gtfsapi.model.RouteBM
import xyz.malkki.gtfsapi.model.StopBM
import xyz.malkki.gtfsapi.model.StopScheduleRowBM
import xyz.malkki.gtfsapi.model.TripInstanceBM
import xyz.malkki.gtfsapi.model.TripScheduleRowBM
import xyz.malkki.gtfsapi.model.VehiclePositionBM
import java.time.LocalDate
import java.time.ZonedDateTime

@RestController
class GtfsController(@Autowired private val gtfsService: GtfsService, @Autowired private val gtfsRtService: GtfsRtService) {
    @QueryMapping
    fun routes(): List<RouteBM>  {
        return gtfsService.getRoutes()
    }

    @QueryMapping
    fun stops(): Collection<StopBM> {
        return gtfsService.getStops()
    }

    @QueryMapping
    fun stop(@Argument id: String): StopBM? {
        return gtfsService.getStopById(id)
    }

    @QueryMapping
    fun stopsNearby(@Argument latitude: Double, @Argument longitude: Double, @Argument radius: Double): List<StopBM> {
        val latLng = LatLng(latitude, longitude)
        require(latLng.validate()) {
            "Invalid coordinates: $latLng"
        }

        return gtfsService.getStopsNearby(latLng, radius)
    }

    @QueryMapping
    fun trip(@Argument id: String, @Argument date: LocalDate): TripInstanceBM? {
        return gtfsService.getTripInstance(id, date)
    }

    @SchemaMapping(typeName = "Route", field = "trips")
    fun routeTrips(route: RouteBM, @Argument from: LocalDate?, @Argument to: LocalDate?): List<TripInstanceBM> {
        return gtfsService.getTripInstancesForRoute(route.routeId, from, to)
    }

    @SchemaMapping(typeName = "TripInstance", field = "route")
    fun tripRoute(tripInstance: TripInstanceBM): RouteBM {
        return gtfsService.getRoute(tripInstance.routeId)!!
    }

    @SchemaMapping(typeName = "TripInstance", field = "scheduleRows")
    fun tripScheduleRows(tripInstanceBM: TripInstanceBM): List<TripScheduleRowBM> {
        return gtfsService.getTripScheduleRows(tripInstanceBM.tripId, tripInstanceBM.date)
    }

    @SchemaMapping(typeName = "TripInstance", field = "vehiclePosition")
    fun tripVehiclePosition(tripInstanceBM: TripInstanceBM): VehiclePositionBM? {
        return gtfsRtService.getVehiclePositions().find {
            it.tripId == tripInstanceBM.tripId && it.tripDate == tripInstanceBM.date
        }
    }

    @SchemaMapping(typeName = "TripInstance", field = "shape")
    fun tripShape(tripInstance: TripInstanceBM): String? {
        return gtfsService.getShapeByTripId(tripInstance.tripId)
    }

    @SchemaMapping(typeName = "VehiclePosition", field = "trip")
    fun vehiclePositionTrip(vehiclePosition: VehiclePositionBM): TripInstanceBM? {
        return gtfsService.getTripInstance(vehiclePosition.tripId, vehiclePosition.tripDate)
    }

    @SchemaMapping(typeName = "VehiclePosition", field = "currentStop")
    fun vehiclePositionCurrentStop(vehiclePosition: VehiclePositionBM): TripScheduleRowBM? {
        val scheduleRows = gtfsService.getTripScheduleRows(vehiclePosition.tripId, vehiclePosition.tripDate)

        val byStopSequence = if (vehiclePosition.currentStopSequence != null) {
            scheduleRows.find { it.sequenceNumber == vehiclePosition.currentStopSequence }
        } else {
            null
        }

        return byStopSequence ?:
            //NOTE: Finding stop by ID can return wrong schedule row if the trip goes through the same stop more than once
            scheduleRows.find { it.stopId == vehiclePosition.stopId }
    }

    @SchemaMapping(typeName = "TripScheduleRow", field = "stop")
    fun tripScheduleRowStop(tripScheduleRow: TripScheduleRowBM): StopBM {
        return gtfsService.getStopById(tripScheduleRow.stopId)!!
    }

    @SchemaMapping(typeName = "Stop", field = "scheduleRows")
    fun stopScheduleRows(stop: StopBM, @Argument max: Int?): List<StopScheduleRowBM> {
        val now = ZonedDateTime.now()

        val scheduleRows = gtfsService.getNextDeparturesFromStop(stop.stopId, now, now.plusDays(1))

        return if (max != null) {
            scheduleRows.take(max)
        } else {
            scheduleRows
        }
    }

    @SchemaMapping(typeName = "StopScheduleRow", field = "trip")
    fun stopScheduleRowTrip(stopScheduleRow: StopScheduleRowBM): TripInstanceBM {
        return gtfsService.getTripInstance(stopScheduleRow.tripId, stopScheduleRow.tripDate)!!
    }

    @SubscriptionMapping
    fun vehiclePositions(): Flux<List<VehiclePositionBM>> {
        return gtfsRtService.getVehiclePositionFlux()
    }
}