package xyz.malkki.gtfsapi.extensions

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.time.Duration
import kotlin.time.toJavaDuration

fun <T> Flux<T>.debounce(duration: Duration): Flux<T> = sampleTimeout {
    Mono.empty<T>().delaySubscription(duration.toJavaDuration())
}