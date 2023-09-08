package xyz.malkki.gtfsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GtfsApiApplication

fun main(args: Array<String>) {
    runApplication<GtfsApiApplication>(*args)
}

