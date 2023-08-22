package xyz.malkki.gtfsapi.extensions

import java.net.http.HttpResponse

val HttpResponse<*>.isSuccessful: Boolean
    get() = statusCode() in 200..299