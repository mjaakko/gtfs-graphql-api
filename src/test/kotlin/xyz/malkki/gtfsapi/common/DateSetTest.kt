package xyz.malkki.gtfsapi.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.random.Random

class DateSetTest {
    @Test
    fun `Test iterating over DateSet`() {
        val dateList = getRandomDates()

        val dateSet = DateSet(dateList)

        assertEquals(dateList, dateSet.iterator().asSequence().toList())
    }

    @Test
    fun `Test iterating over DateSet with descending iterator`() {
        val dateList = getRandomDates()

        val dateSet = DateSet(dateList)

        assertEquals(dateList.reversed(), dateSet.descendingIterator().asSequence().toList())
    }

    @Test
    fun `Test subset of DateSet` () {
        val dateList = getRandomDates()

        val from = dateList[3].plusDays(1)
        val to = dateList[7].plusDays(1)

        val dateSet = DateSet(dateList)
        val subset = dateSet.subSet(from, true, to, true)

        assertTrue(subset.size < dateSet.size)
        assertTrue(dateSet.containsAll(subset))

        subset.forEach { date ->
            assertTrue(date >= from)
            assertTrue(date <= to)
        }
    }

    @Test
    fun `Test tail set of DateSet` () {
        val dateList = getRandomDates()

        val from = dateList[3].plusDays(1)

        val dateSet = DateSet(dateList)
        val tailset = dateSet.tailSet(from, true)

        assertTrue(tailset.size < dateSet.size)
        assertTrue(dateSet.containsAll(tailset))

        tailset.forEach { date ->
            assertTrue(date >= from)
        }
    }

    @Test
    fun `Test head set of DateSet` () {
        val dateList = getRandomDates()

        val to = dateList[7].plusDays(1)

        val dateSet = DateSet(dateList)
        val headset = dateSet.headSet(to, true)

        assertTrue(headset.size < dateSet.size)
        assertTrue(dateSet.containsAll(headset))

        headset.forEach { date ->
            assertTrue(date <= to)
        }
    }

    @Test
    fun `Test DateSet contains all dates`() {
        val dateList = getRandomDates()

        val dateSet = DateSet(dateList)

        dateList.forEach {
            assertTrue { it in dateSet }
        }

        assertTrue { dateSet.containsAll(dateList) }
    }

    @Test
    fun `Test isEmpty returns true for empty DateSet`() {
        val dateSet = DateSet(emptyList())

        assertTrue(dateSet.isEmpty())
    }

    @Test
    fun `Test first returns first date of DateSet`() {
        val dateList = getRandomDates()

        val first = dateList.min()

        val dateSet = DateSet(dateList)

        assertEquals(first, dateSet.first())
    }

    @Test
    fun `Test last returns last date of DateSet`() {
        val dateList = getRandomDates()

        val last = dateList.max()

        val dateSet = DateSet(dateList)

        assertEquals(last, dateSet.last())
    }

    private fun getRandomDates(): List<LocalDate> {
        val today = LocalDate.now()

        return (1..10).fold(listOf(today)) { acc, _ ->
            val min = acc.last()

             acc + listOf(min.plusDays(Random.Default.nextLong(1, 5)))
        }
    }
}