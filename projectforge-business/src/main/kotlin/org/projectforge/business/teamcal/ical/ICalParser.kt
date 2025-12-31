/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.teamcal.ical

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.component.VEvent
import org.projectforge.business.teamcal.ical.VEventUtils.convertToEventDO
import org.projectforge.business.teamcal.event.model.TeamEventDO
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.Reader
import java.io.StringReader
import java.nio.charset.Charset

/**
 * Parses iCalendar data from an input stream.
 */
class ICalParser {
    var teamEvents: List<TeamEventDO>? = null
        private set

    var vEvents: List<VEvent>? = null
        private set

    fun parse(byteArray: ByteArray): List<TeamEventDO> {
        val inputStream = ByteArrayInputStream(byteArray)
        return parse(inputStream)
    }

    fun parse(content: String): List<TeamEventDO> {
        return parse(StringReader(content))
    }

    @JvmOverloads
    fun parse(inputStream: InputStream, charset: Charset = Charsets.UTF_8): List<TeamEventDO> {
        inputStream.reader(charset).use { reader ->
            return parse(reader)
        }
    }

    /**
     * Fetches and parses iCalendar data from an input stream.
     * @param inputStream The input stream to read from.
     * @return A list of [TeamEventDO] objects parsed from the iCalendar data.
     */
    fun parse(reader: Reader): List<TeamEventDO> {
        val builder = CalendarBuilder()
        val calendar = builder.build(reader)
        val newVEvents = mutableListOf<VEvent>()
        val newTeamEvents = mutableListOf<TeamEventDO>()
        // Iterate through components in the calendar
        calendar.getComponents<VEvent>(Component.VEVENT).forEach { component ->
            newTeamEvents.add(convertToEventDO(component))
        }
        teamEvents = newTeamEvents
        vEvents = newVEvents
        return newTeamEvents
    }
}
