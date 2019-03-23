package org.projectforge.rest.core

import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.servlet.http.HttpSession

/**
 * For persisting list filters.
 */
@Service
class ListFilterService {
    private val log = org.slf4j.LoggerFactory.getLogger(ListFilterService::class.java)

    @Autowired
    private val userPreferencesService: UserPreferencesService? = null

    fun getSearchFilter(session: HttpSession, filterClazz: Class<out BaseSearchFilter>): BaseSearchFilter {
        val filter = userPreferencesService!!.getEntry(session, filterClazz.name + ":Filter")
        if (filter != null) {
            if (filter.javaClass == filterClazz) {
                try {
                    return filter as BaseSearchFilter
                } catch (ex: ClassCastException) {
                    // No output needed, info message follows:
                }
                // Probably a new software release results in an incompability of old and new filter format.
                log.info(
                        "Could not restore filter from user prefs: (old) filter type "
                                + filter.javaClass.getName()
                                + " is not assignable to (new) filter type "
                                + filterClazz.javaClass.getName()
                                + " (OK, probably new software release).")
            }
        }
        val result = filterClazz.newInstance()
        result.reset()
        userPreferencesService.putEntry(session, filterClazz.name + ":Filter", result, true)
        return result
    }
}
