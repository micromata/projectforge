/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.business.calendar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.projectforge.business.user.UserPrefDao
import org.projectforge.favorites.Favorites
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.*

class CalendarFilterFavoritesTest {
    @Test
    fun jsonTest() {
        val json = "{\"set\":[" +
                "{\"type\":\"org.projectforge.business.calendar.CalendarFilter\",\"name\":\"Abwesenheiten\",\"defaultCalendarId\":-1,\"calendarIds\":[1240530]}," +
                "{\"type\":\"org.projectforge.business.calendar.CalendarFilter\",\"name\":\"Alex\",\"id\":1,\"defaultCalendarId\":-1,\"timesheetUserId\":48,\"showTimesheets\":true,\"showBreaks\":true,\"calendarIds\":[1722123]}," +
                "{\"type\":\"org.projectforge.business.calendar.CalendarFilter\",\"name\":\"Filter\",\"id\":2,\"defaultCalendarId\":-1,\"calendarIds\":[1292975]}," +
                "{\"type\":\"org.projectforge.business.calendar.CalendarFilter\",\"name\":\"Kai\",\"id\":3,\"defaultCalendarId\":-1,\"showBirthdays\":true,\"showStatistics\":true,\"timesheetUserId\":2,\"showTimesheets\":true,\"showBreaks\":true,\"showPlanning\":true,\"calendarIds\":[1240526,1240528,1245916,1245918,1285741,1292975],\"invisibleCalendars\":[1245916,1245918]}," +
                "{\"type\":\"org.projectforge.business.calendar.CalendarFilter\",\"name\":\"Kai Zeitbuchungen\",\"id\":4,\"defaultCalendarId\":-1,\"showBirthdays\":true,\"showStatistics\":true,\"timesheetUserId\":2,\"showTimesheets\":true,\"showBreaks\":true,\"calendarIds\":[1240526,1240530,15573458],\"invisibleCalendars\":[1240526,1240530]}," +
                "{\"type\":\"org.projectforge.business.calendar.CalendarFilter\",\"name\":\"Kollegen\",\"id\":5,\"defaultCalendarId\":-1,\"showStatistics\":true,\"timesheetUserId\":40,\"showTimesheets\":true}," +
                "{\"type\":\"org.projectforge.business.calendar.CalendarFilter\",\"name\":\"Micromata\",\"id\":6,\"defaultCalendarId\":1240538,\"calendarIds\":[1240530,1240532,1240538,1265941,1272155,1959521]}," +
                "{\"type\":\"org.projectforge.business.calendar.CalendarFilter\",\"name\":\"Mitarbeiter\",\"id\":7,\"defaultCalendarId\":-1,\"showStatistics\":true,\"timesheetUserId\":141925,\"showTimesheets\":true,\"showBreaks\":true}," +
                "{\"type\":\"org.projectforge.business.calendar.CalendarFilter\",\"name\":\"Standard\",\"id\":8,\"defaultCalendarId\":-1,\"showStatistics\":true,\"timesheetUserId\":2,\"showTimesheets\":true,\"showBreaks\":true,\"showPlanning\":true,\"calendarIds\":[1240526,1240528],\"invisibleCalendars\":[1240528]},"+
                "{\"type\":\"org.projectforge.business.calendar.CalendarFilter\",\"name\":\"Stéphanie\",\"id\":9,\"defaultCalendarId\":-1,\"calendarIds\":[1245916,1245918]}," +
                "{\"type\":\"org.projectforge.business.calendar.CalendarFilter\",\"name\":\"Urlaub\",\"id\":10,\"defaultCalendarId\":-1,\"showBreaks\":true,\"calendarIds\":[1240530]}]}"
        val favorites = UserPrefDao.getObjectMapper().readValue(json, Favorites::class.java)
        assertEquals(11, favorites.favoriteNames.size)
    }

    @Test
    fun autoNameTest() {
        val favs = Favorites<CalendarFilter>()
        favs.add(CalendarFilter())
        val prefix = favs.getElementAt(0)!!.name ?: fail("prefix can't be null")
        assertEquals(translate("favorite.untitled"), prefix)
        favs.add(CalendarFilter())
        assertEquals("$prefix 1", favs.getElementAt(1)!!.name)
        favs.add(CalendarFilter(name = "My favorite"))
        favs.add(CalendarFilter(name = "My favorite"))
        favs.add(CalendarFilter(name = "My favorite"))
        assertEquals("My favorite 1", favs.getElementAt(3)!!.name)
        assertEquals("My favorite 2", favs.getElementAt(4)!!.name)

        assertEquals(1, favs.getElementAt(0)!!.id)
        assertEquals(2, favs.getElementAt(1)!!.id)
        assertEquals(3, favs.getElementAt(2)!!.id)
        assertEquals(4, favs.getElementAt(3)!!.id)
        assertEquals(5, favs.getElementAt(4)!!.id)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            ConfigXml.createForJunitTests()
            val user = PFUserDO()
            user.locale = Locale.GERMAN
            ThreadLocalUserContext.setUserContext(UserContext(user, null))
        }
    }
}
