package xyz.malkki.gtfsapi.gtfs

import com.github.benmanes.caffeine.cache.Caffeine
import xyz.malkki.gtfs.model.Calendar
import xyz.malkki.gtfs.model.CalendarDate
import java.time.LocalDate
import java.util.*

class ServiceDates(calendars: Collection<Calendar>, calendarDates: Collection<CalendarDate>) {
    private val calendarsByServiceId = calendars.groupBy { it.serviceId }
    private val calendarDatesByServiceId = calendarDates.groupBy { it.serviceId }

    private val dateSetCache = Caffeine.newBuilder()
        /*.maximumWeight(15 * 1024 * 1024 * 8) //15MB
        .weigher { _: String, set: DateSet ->
            set.bitSize
        }*/
        .build<String, NavigableSet<LocalDate>> { serviceId ->
            TreeSet(getDatesForService(serviceId))
        }

    private fun getDatesForService(serviceId: String): Collection<LocalDate> {
        val dates = (calendarsByServiceId[serviceId] ?: emptyList())
            .flatMap { it.toList() }
            .toList()

        val calendarDates = calendarDatesByServiceId[serviceId] ?: emptyList()

        val addedDates = calendarDates
            .filter { it.exceptionType == CalendarDate.EXCEPTION_TYPE_ADDED }
            .map { it.date }
        val removedDates = calendarDates
            .filter { it.exceptionType == CalendarDate.EXCEPTION_TYPE_REMOVED }
            .map { it.date }
            .toSet()

        return (dates + addedDates).filter { it !in removedDates }
    }

    fun getDatesForServiceId(serviceId: String): NavigableSet<LocalDate> = dateSetCache[serviceId]
}