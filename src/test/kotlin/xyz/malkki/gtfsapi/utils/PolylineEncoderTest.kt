package xyz.malkki.gtfsapi.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import xyz.malkki.gtfsapi.common.LatLng

class PolylineEncoderTest {
    @Test
    fun `Test encoding polyline`() {
        val points = listOf(
            LatLng(60.16187, 24.95898),
            LatLng(60.09154, 19.92829),
            LatLng(59.35048, 18.11385)
        )

        val encodedPolyline = encodePolyline(points)

        //Verified with https://www.freemaptools.com/create-and-plot-encoded-polyline-on-map.htm that the result is correct
        assertEquals("ujenJsxiwCpvLxpu]rvoCfkaJ", encodedPolyline)
    }
}