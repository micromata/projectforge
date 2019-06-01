package org.projectforge.rest.calendar

/**
 * Persist the user's list of calendar filters. The user may configure a list of filter and my switch the active
 * used/displayed calendar filter.
 */
class CalendarFilterList() {
    private val log = org.slf4j.LoggerFactory.getLogger(CalendarFilterList::class.java)

    val list: MutableList<CalendarFilter> = mutableListOf()

    internal fun getActiveFilter(activeFilterIndex: Int): CalendarFilter? {
        if (activeFilterIndex < 0) return null // No filter is marked as active.
        if (activeFilterIndex < list.size) {
            // Get the user's active filter:
            return list[activeFilterIndex]
        }
        log.error("Active filter index #$activeFilterIndex is out of array bounds [0..${list.size - 1}]")
        return null
    }
}
