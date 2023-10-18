package xyz.malkki.gtfsapi

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import xyz.malkki.gtfs.serialization.parser.ZipGtfsFeedParser
import xyz.malkki.gtfsapi.extensions.isSuccessful
import xyz.malkki.gtfsapi.gtfs.GtfsIndex
import xyz.malkki.gtfsapi.utils.timer
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.deleteIfExists
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

private val log = KotlinLogging.logger {  }

@Component
@ConditionalOnProperty(name = ["gtfs.feed.url"])
class HttpGtfsIndexProvider(
    @Autowired private val httpClient: HttpClient,
    @Value("\${gtfs.feed.url}") private val gtfsUrlString: String,
    @Value("\${gtfs.updateInterval}") gtfsUpdateInterval: Duration
) : GtfsIndexProvider {
    override val gtfsIndexFlux = timer(gtfsUpdateInterval.toKotlinDuration())
        .map { downloadGtfs() }
        .map { gtfsPath ->
            try {
                val gtfsParser = ZipGtfsFeedParser(gtfsPath)

                gtfsParser.use {
                    GtfsIndex(
                        it.parseAgencies().toList(),
                        it.parseRoutes().toList(),
                        it.parseTrips().toList(),
                        it.parseStops().toList(),
                        it.parseStopTimes().toList(),
                        it.parseCalendars().toList(),
                        it.parseCalendarDates().toList(),
                        it.parseShapes().toList()
                    )
                }
            } finally {
                gtfsPath.deleteIfExists()
            }
        }
        .replay(1)
        .refCount(1, 1.seconds.toJavaDuration())

    @PostConstruct
    fun waitForIndex() {
        gtfsIndexFlux.blockFirst(Duration.ofMinutes(5))
    }

    private fun downloadGtfs(): Path {
        val tempFile = kotlin.io.path.createTempFile("gtfs", ".zip")

        try {
            log.info {
                "Downloading GTFS from $gtfsUrlString to $tempFile"
            }

            val (response, duration) = measureTimedValue {
                httpClient.send(HttpRequest.newBuilder(URI.create(gtfsUrlString)).GET().build(), BodyHandlers.ofFile(tempFile))
            }

            if (response.isSuccessful) {
                log.info {
                    "Downloaded GTFS from $gtfsUrlString in ${duration.toString(DurationUnit.SECONDS, 1)}"
                }

                return tempFile
            } else {
                throw IOException("Failed to download GTFS from $gtfsUrlString, HTTP status: ${response.statusCode()}")
            }
        } catch (ex: Exception) {
            tempFile.deleteIfExists()

            throw ex
        }
    }
}