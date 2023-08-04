package xyz.malkki.gtfsapi.utils

import xyz.malkki.gtfsapi.common.LatLng

private fun encodeSignedNumber(num: Int): String {
    var sgnNum = num shl 1
    if (num < 0) {
        sgnNum = sgnNum.inv()
    }
    return encodeNumber(sgnNum)
}

private fun encodeNumber(num: Int): String {
    var num = num
    var encodeString = ""

    while (num >= 0x20) {
        val nextValue = (0x20 or (num and 0x1f)) + 63
        encodeString += nextValue.toChar()
        num = num shr 5
    }

    num += 63
    encodeString += num.toChar()

    return encodeString
}

fun encodePolyline(points: List<LatLng>): String {
    val pointsE5 = points.map { it.latitude * 1E5 to it.longitude * 1E5 }

    return pointsE5
        .mapIndexed { index, point ->
            println("Encoding $index")
            val prev = if (index > 0) {
                pointsE5[index-1]
            } else {
                0.0 to 0.0
            }

            val encoded = "${encodeSignedNumber((point.first - prev.first).toInt())}${encodeSignedNumber((point.second - prev.second).toInt())}"
            println("Encoded to ${encoded}")
            encoded
        }
        .joinToString("")
}