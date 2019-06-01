package org.projectforge.rest.calendar

import org.projectforge.framework.i18n.translate

/**
 * Persist the user's list of calendar filters. The user may configure a list of favorite filter and my apply one
 * by choosing from a drop down list.
 */
class CalendarFilterFavorites() {
    private val log = org.slf4j.LoggerFactory.getLogger(CalendarFilterFavorites::class.java)

    private val list: MutableList<CalendarFilter> = mutableListOf()

    fun add(filter: CalendarFilter) {
        list.add(filter)
        fixNames()
    }

    fun remove(name: String) {
        fixNames()
        list.removeIf { it.name == name }
    }

    /**
     * Fixes empty names and doublets of names.
     */
    private fun fixNames() {
        val namesSet = mutableSetOf<String>()
        list.forEach {
            if (it.name.isNullOrBlank())
                it.name = getAutoName() // Fix empty names
            if (namesSet.contains(it.name)) {
                // Doublet found
                it.name = getAutoName(it.name)
            }
            namesSet.add(it.name)
        }
    }

    fun getAutoName(prefix: String? = null): String {
        var _prefix = prefix ?: translate("plugins.teamcal.calendar.filterDialog.newTemplateName")
        if (list.isEmpty()) {
            return _prefix
        }
        val existingNames = list.map { it.name }
        if (!existingNames.contains(_prefix))
            return _prefix
        for (i in 1..30) {
            val name = "$_prefix $i"
            if (!existingNames.contains(name))
                return name
        }
        return _prefix // Giving up, 1..30 are already used.
    }


    internal fun getFavoriteNames(): List<String> {
        fixNames()
        return list.map { it.name }
    }

    internal fun getFilter(index: Int): CalendarFilter? {
        if (index < 0) return null // No filter is marked as active.
        if (index < list.size) {
            // Get the user's active filter:
            return list[index]
        }
        log.error("Favorite filter index #$index is out of array bounds [0..${list.size - 1}].")
        return null
    }

    internal fun getFilter(name: String): CalendarFilter? {
        list.forEach {
            if (name == it.name)
                return it
        }
        log.error("Favorite filter named '$name' not found.")
        return null
    }
}
