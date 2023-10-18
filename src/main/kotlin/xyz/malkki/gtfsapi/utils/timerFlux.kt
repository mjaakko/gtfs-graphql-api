package xyz.malkki.gtfsapi.utils

import reactor.core.publisher.Flux
import kotlin.concurrent.thread
import kotlin.time.Duration

fun timer(interval: Duration): Flux<Long> = Flux.create { sink ->
    val emitterThread = thread(start = true, isDaemon = true, name = "Timer") {
        var i = 0L

        while (true) {
            try {
                sink.next(i++)

                Thread.sleep(interval.inWholeMilliseconds)
            } catch (interrupt: InterruptedException) {
                break
            }
        }
    }

    sink.onDispose(emitterThread::interrupt)
}