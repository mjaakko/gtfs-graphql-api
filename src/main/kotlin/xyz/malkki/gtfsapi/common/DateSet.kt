package xyz.malkki.gtfsapi.common

import java.time.LocalDate
import java.util.*

/**
 * Optimized set for storing LocalDates. Not modifiable, all modify methods throw [UnsupportedOperationException]
 */
class DateSet : NavigableSet<LocalDate> {
    private val first: LocalDate?
    private val firstEpochDay: Long?

    private val bitset: BitSet

    constructor(dates: Collection<LocalDate>) {
        first = dates.minOrNull()
        firstEpochDay = first?.toEpochDay()

        bitset = BitSet()

        dates.forEach { date ->
            bitset.set(getIndexByDate(date))
        }
    }

    private constructor(first: LocalDate, bitset: BitSet) {
        this.first = first
        this.firstEpochDay = first.toEpochDay()

        this.bitset = bitset
    }

    /**
     * Number of bits needed to represent this date set
     */
    val bitSize: Int
        get() = bitset.size()

    private fun getIndexByDate(date: LocalDate): Int {
        return (date.toEpochDay() - firstEpochDay!!).toInt()
    }

    private fun getDateByIndex(index: Int): LocalDate {
        return LocalDate.ofEpochDay(firstEpochDay!! + index)
    }

    override fun add(element: LocalDate?): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(elements: Collection<LocalDate>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        throw UnsupportedOperationException()
    }

    override fun iterator(): MutableIterator<LocalDate> = object : MutableIterator<LocalDate> {
        private var index = if (first == null) { -1 } else { 0 }

        override fun hasNext(): Boolean {
            return index != -1
        }

        override fun next(): LocalDate {
            val date = getDateByIndex(index)

            index = bitset.nextSetBit(index + 1)

            return date
        }

        override fun remove() {
            throw UnsupportedOperationException()
        }
    }

    override fun isEmpty(): Boolean = first == null

    override fun comparator(): Comparator<in LocalDate> = Comparator.naturalOrder()

    override fun first(): LocalDate = first!!

    override fun last(): LocalDate = getDateByIndex(bitset.length() - 1)

    override fun pollFirst(): LocalDate? {
        throw UnsupportedOperationException()
    }

    override fun pollLast(): LocalDate? {
        throw UnsupportedOperationException()
    }

    /**
     * Throws UnsupportedOperationException
     */
    override fun descendingSet(): NavigableSet<LocalDate> {
        throw UnsupportedOperationException()
    }

    override fun descendingIterator(): MutableIterator<LocalDate> = object : MutableIterator<LocalDate> {
        private var index = if (first == null) { -1 } else { bitset.length() - 1 }

        override fun hasNext(): Boolean {
            return index != -1
        }

        override fun next(): LocalDate {
            val date = getDateByIndex(index)

            index = bitset.previousSetBit(index - 1)

            return date
        }

        override fun remove() {
            throw UnsupportedOperationException()
        }
    }

    override val size: Int
        get() = bitset.cardinality()

    override fun higher(date: LocalDate?): LocalDate? {
        return date?.let { getIndexByDate(it) }
            ?.let { bitset.nextSetBit(it + 1) }
            ?.let { getDateByIndex(it) }
    }

    override fun ceiling(date: LocalDate?): LocalDate? {
        return date?.let { getIndexByDate(it) }
            ?.let { bitset.nextSetBit(it) }
            ?.let { getDateByIndex(it) }
    }

    override fun floor(date: LocalDate?): LocalDate? {
        if (first == null || date == null || date < first) {
            return null
        }

        return getIndexByDate(date)
            .let { bitset.previousSetBit(it) }
            .let { getDateByIndex(it) }
    }

    override fun lower(date: LocalDate?): LocalDate? {
        if (first == null || date == null || date <= first) {
            return null
        }

        return getIndexByDate(date)
            .let { bitset.previousSetBit(it) }
            .let { getDateByIndex(it) }
    }

    override fun tailSet(fromElement: LocalDate?): SortedSet<LocalDate> = tailSet(fromElement, true)

    override fun tailSet(fromElement: LocalDate?, inclusive: Boolean): NavigableSet<LocalDate> {
        if (fromElement == null || first == null) {
            return Collections.emptyNavigableSet()
        }

        return subSet(fromElement, inclusive, getDateByIndex(bitset.length() - 1), true)
    }

    override fun headSet(toElement: LocalDate?): SortedSet<LocalDate> = headSet(toElement, true)

    override fun headSet(toElement: LocalDate?, inclusive: Boolean): NavigableSet<LocalDate> {
        if (toElement == null || first == null) {
            return Collections.emptyNavigableSet()
        }

        return subSet(first, true, toElement, true)
    }

    override fun subSet(fromElement: LocalDate?, toElement: LocalDate?): SortedSet<LocalDate> = subSet(fromElement, true, toElement, false)

    override fun subSet(
        fromElement: LocalDate?,
        fromInclusive: Boolean,
        toElement: LocalDate?,
        toInclusive: Boolean
    ): NavigableSet<LocalDate> {
        if (fromElement == null || toElement == null) {
            return Collections.emptyNavigableSet()
        }

        val fromIndex = if (fromElement < first) {
            0
        } else {
            getIndexByDate(fromElement)
        }
        val toIndex = if (toElement < first) {
            0
        } else {
            getIndexByDate(toElement)
        }

        val firstSetBit = bitset.nextSetBit(fromIndex + if (fromInclusive) { 0 } else { 1 })
        val lastSetBit = bitset.previousSetBit(toIndex - if (toInclusive) { 0 } else { 1 })

        val firstDate = getDateByIndex(firstSetBit)
        val subset = bitset.get(firstSetBit, lastSetBit)

        return DateSet(firstDate, subset)
    }

    override fun containsAll(elements: Collection<LocalDate>): Boolean {
        return elements.all { contains(it) }
    }

    override fun contains(element: LocalDate?): Boolean {
        if (element == null || first == null || element < first) {
            return false
        }

        return bitset.get(getIndexByDate(element))
    }

    override fun retainAll(elements: Collection<LocalDate>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun removeAll(elements: Collection<LocalDate>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(element: LocalDate?): Boolean {
        throw UnsupportedOperationException()
    }
}