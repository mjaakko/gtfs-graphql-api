package xyz.malkki.gtfsapi.extensions

import xyz.malkki.gtfs.model.Shape
import xyz.malkki.gtfs.model.Stop
import xyz.malkki.gtfs.model.StopTime
import xyz.malkki.gtfsapi.common.LatLng

val Stop.location: LatLng?
    get() = if (stopLat != null && stopLon != null) { LatLng(stopLat!!, stopLon!!) } else { null }

val Shape.location: LatLng
    get() = LatLng(shapePtLat, shapePtLon)

val StopTime.hasDropOff: Boolean
    //1 = no drop off https://gtfs.org/schedule/reference/#stop_timestxt
    get() = dropOffType != 1

val StopTime.hasPickUp: Boolean
    get() = pickupType != 1
