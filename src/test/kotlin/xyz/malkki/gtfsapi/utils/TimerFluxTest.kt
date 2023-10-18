package xyz.malkki.gtfsapi.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class TimerFluxTest {
    @Test
    fun `Test timer flux`() {
        val timer = timer(500.milliseconds)

        val collected = timer.buffer(900.milliseconds.toJavaDuration()).take(1).blockFirst(2.seconds.toJavaDuration())

        assertEquals(2, collected?.size)
    }
}