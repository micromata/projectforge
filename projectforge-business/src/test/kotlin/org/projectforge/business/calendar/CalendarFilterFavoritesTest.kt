package org.projectforge.business.calendar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.favorites.Favorites
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.*

class CalendarFilterFavoritesTest {
    @Test
    fun autoNameTest() {
        val favs = Favorites<CalendarFilter>()
        favs.add(CalendarFilter())
        val prefix = favs.getElementAt(0)!!.name
        assertTrue(prefix.startsWith("???")) // Translations not available
        assertTrue(prefix.endsWith("???")) // Translations not available
        favs.add(CalendarFilter())
        assertEquals("$prefix 1", favs.getElementAt(1)!!.name)
        favs.add(CalendarFilter(name = "My favorite"))
        favs.add(CalendarFilter(name = "My favorite"))
        favs.add(CalendarFilter(name = "My favorite"))
        assertEquals("My favorite 1", favs.getElementAt(3)!!.name)
        assertEquals("My favorite 2", favs.getElementAt(4)!!.name)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            ConfigXml(".")
            val user = PFUserDO()
            user.locale = Locale.GERMAN
            ThreadLocalUserContext.setUserContext(UserContext(user, null))
        }
    }
}
