package xyz.malkki.gtfsapi

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import xyz.malkki.gtfs.serialization.parser.ZipGtfsFeedParser
import xyz.malkki.gtfsapi.extensions.debounce
import xyz.malkki.gtfsapi.gtfs.GtfsIndex
import xyz.malkki.gtfsapi.utils.watchFile
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Component
class GtfsIndexProvider(@Value("\${gtfs.feed.path}") gtfsPathString: String) {
    private val gtfsPath = Paths.get(gtfsPathString)

    val gtfsIndexFlux = gtfsPath.watchFile(StandardWatchEventKinds.ENTRY_MODIFY)
        .debounce(500.milliseconds)
        .map {  }
        .startWith(Unit)
        .map {
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
        }
        .replay(1)
        .refCount()

    @PostConstruct
    fun waitForIndex() {
        gtfsIndexFlux.blockFirst(Duration.ofMinutes(5))
    }
}