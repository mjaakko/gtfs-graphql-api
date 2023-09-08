package xyz.malkki.gtfsapi

import com.google.transit.realtime.GtfsRealtime.VehiclePosition
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import xyz.malkki.gtfs.utils.GtfsDateFormat
import xyz.malkki.gtfsapi.model.VehiclePositionBM
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset


@Service
class GtfsRtService(@Autowired private val vehiclePositionService: VehiclePositionService)  {
    private val vehiclePositionFlux = vehiclePositionService.getVehiclePositions()

    fun getVehiclePositions(): List<VehiclePositionBM> = vehiclePositionFlux.blockFirst()!!.map(::mapToBM)

    fun getVehiclePositionFlux(): Flux<List<VehiclePositionBM>> {
        return vehiclePositionFlux
            .flatMap { Mono.just(it.map(::mapToBM)) }
            .transform { sink ->
                var previousTimestamps: Map<String, OffsetDateTime>? = null

                sink
                    .filter {
                        if (previousTimestamps == null) {
                            return@filter true
                        }

                        val currentTimestamps = it.associate { vp -> vp.vehicleId to vp.timestamp }

                        previousTimestamps!!.keys != currentTimestamps.keys
                                || currentTimestamps.any { (vehicleId, currentTimestamp) -> currentTimestamp > previousTimestamps!![vehicleId] }
                    }
                    .doOnEach {
                        previousTimestamps = it.get()!!.associate { vp -> vp.vehicleId to vp.timestamp }
                    }
            }
    }

    private fun mapToBM(vehiclePosition: VehiclePosition): VehiclePositionBM {
        return VehiclePositionBM(
            vehiclePosition.trip.tripId,
            GtfsDateFormat.parseFromString(vehiclePosition.trip.startDate),
            vehiclePosition.vehicle.id,
            vehiclePosition.vehicle.label.takeIf { vehiclePosition.vehicle.hasLabel() },
            vehiclePosition.position.latitude,
            vehiclePosition.position.longitude,
            vehiclePosition.position.bearing.takeIf { vehiclePosition.position.hasBearing() },
            vehiclePosition.position.speed.takeIf { vehiclePosition.position.hasSpeed() },
            vehiclePosition.currentStatus.toString(),
            vehiclePosition.currentStopSequence.takeIf { vehiclePosition.hasCurrentStopSequence() },
            vehiclePosition.stopId.takeIf { vehiclePosition.hasStopId() },
            Instant.ofEpochSecond(vehiclePosition.timestamp).atOffset(ZoneOffset.UTC)
        )
    }
}