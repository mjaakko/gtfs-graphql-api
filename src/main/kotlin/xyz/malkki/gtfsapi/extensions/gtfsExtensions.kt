package xyz.malkki.gtfsapi.extensions

import xyz.malkki.gtfs.model.Shape
import xyz.malkki.gtfs.model.Stop
import xyz.malkki.gtfsapi.common.LatLng

val Stop.location: LatLng?
    get() = if (stopLat != null && stopLon != null) { LatLng(stopLat!!, stopLon!!) } else { null }

val Shape.location: LatLng
    get() = LatLng(shapePtLat, shapePtLon)