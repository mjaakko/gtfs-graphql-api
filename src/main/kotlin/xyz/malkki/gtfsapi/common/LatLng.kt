package xyz.malkki.gtfsapi.common

import java.lang.Math.toDegrees
import java.lang.Math.toRadians
import kotlin.math.*


data class LatLng(val latitude: Double, val longitude: Double) {
    companion object {
        private const val EARTH_RADIUS_IN_METERS = 6371 * 1000

        private const val MAX_LATITUDE = 90.0
        private const val MIN_LATITUDE = -90.0
        private const val MAX_LONGITUDE = 180.0
        private const val MIN_LONGITUDE = -180.0
    }

    /**
     * Checks it the coordinate is valid
     *
     * @return true if the coordinate is valid, false if not
     */
    fun validate(): Boolean {
        return latitude in (MIN_LATITUDE..MAX_LATITUDE) && longitude in (MIN_LONGITUDE..MAX_LONGITUDE)
    }

    /**
     * @return Distance to other coordinate in meters
     */
    fun distanceTo(other: LatLng): Double {
        val latDistance = toRadians(other.latitude - latitude)
        val lonDistance = toRadians(other.longitude - longitude)

        val a = (sin(latDistance / 2) * sin(latDistance / 2)
                + (cos(toRadians(latitude)) * cos(toRadians(other.latitude))
                * sin(lonDistance / 2) * sin(lonDistance / 2)))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_IN_METERS * c
    }

    /**
     * Calculates destination coordinate when traveling from this coordinate
     *
     * @param distance Distance in metres
     * @param bearing Bearing in degrees from north
     */
    fun displace(distance: Double, bearing: Double): LatLng {
        val angDist = distance / EARTH_RADIUS_IN_METERS

        val latitudeRadians = toRadians(latitude)
        val longitudeRadians = toRadians(longitude)

        val bearingRadians = toRadians(bearing)

        val lat2 = asin(sin(latitudeRadians)*cos(angDist) + cos(latitudeRadians)*sin(angDist)*cos(bearingRadians))
        val lon2 = longitudeRadians + atan2(sin(bearingRadians)*sin(angDist)*cos(latitudeRadians), cos(angDist) - sin(latitudeRadians) * sin(lat2))

        return LatLng(toDegrees(lat2), ((toDegrees(lon2) + 540) % 360) - 180)
    }
}
