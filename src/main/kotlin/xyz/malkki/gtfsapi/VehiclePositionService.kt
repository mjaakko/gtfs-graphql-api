package xyz.malkki.gtfsapi

import com.google.transit.realtime.GtfsRealtime
import reactor.core.publisher.Flux

interface VehiclePositionService {
    fun getVehiclePositions(): Flux<List<GtfsRealtime.VehiclePosition>>
}