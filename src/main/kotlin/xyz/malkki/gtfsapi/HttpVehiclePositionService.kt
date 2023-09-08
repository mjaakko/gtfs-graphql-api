package xyz.malkki.gtfsapi

import com.google.transit.realtime.GtfsRealtime
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import xyz.malkki.gtfsapi.extensions.isSuccessful
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private val log = KotlinLogging.logger {}

@Service
class HttpVehiclePositionService(
    @Autowired private val httpClient: HttpClient,
    @Value("\${gtfsRt.vehiclePositions.url}") private val gtfsRtUrl: String
) : VehiclePositionService {
    private val vehiclePositionSink: Sinks.Many<List<GtfsRealtime.VehiclePosition>> = Sinks.many().replay().latestOrDefault(emptyList())

    @Scheduled(fixedDelayString = "\${gtfsRt.vehiclePositions.updateInterval:1000}")
    fun updateVehiclePositions() {
        val request = HttpRequest.newBuilder(URI.create(gtfsRtUrl)).GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

        if (response.isSuccessful) {
            val feedMessage = response.body().buffered().use {
                GtfsRealtime.FeedMessage.parseFrom(it)
            }

            val vehiclePositions = feedMessage.entityList
                .filter { it.hasVehicle() }
                .map { it.vehicle }
                .toList()

            log.debug("Found ${vehiclePositions.size} vehicle positions from GTFS-RT feed")

            vehiclePositionSink.tryEmitNext(vehiclePositions)
        } else {
            log.warn("HTTP request to ${request.uri()} failed, status: ${response.statusCode()}")
        }
    }

    override fun getVehiclePositions(): Flux<List<GtfsRealtime.VehiclePosition>> = vehiclePositionSink.asFlux()
}