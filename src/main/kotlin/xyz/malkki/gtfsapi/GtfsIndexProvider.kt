package xyz.malkki.gtfsapi

import reactor.core.publisher.Flux
import xyz.malkki.gtfsapi.gtfs.GtfsIndex

interface GtfsIndexProvider {
    val gtfsIndexFlux: Flux<GtfsIndex>
}