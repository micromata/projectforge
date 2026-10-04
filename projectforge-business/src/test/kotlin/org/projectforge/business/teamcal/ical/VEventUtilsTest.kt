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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.common.extensions.isoString
import org.projectforge.framework.time.PFDateTime
import org.projectforge.business.test.TestSetup
import java.time.Month

class VEventUtilsTest {
    @Test
    fun `test parsing of ics`() {
        val vEvent = VEventUtils.parseVEventFromIcs(testIcs)
        Assertions.assertNotNull(vEvent)
        val teamEvent = VEventUtils.convertToEventDO(vEvent!!)
        Assertions.assertNotNull(teamEvent)
        Assertions.assertEquals("Team Meeting", teamEvent.subject)
        Assertions.assertEquals("Discuss quarterly goals.", teamEvent.note)
        Assertions.assertEquals("Conference Room", teamEvent.location)
        Assertions.assertEquals("2024-11-15T09:00:00Z", teamEvent.startDate.isoString())
        Assertions.assertEquals("2024-11-15T10:00:00Z", teamEvent.endDate.isoString())
        Assertions.assertEquals("2024-11-01T12:00:00Z", teamEvent.dtStamp.isoString())
    }

    @Test
    fun `test parsing of ics with time zones`() {
        val vEvent = VEventUtils.parseVEventFromIcs(testIcsWithTimeZone)
        Assertions.assertNotNull(vEvent)
        val teamEvent = VEventUtils.convertToEventDO(vEvent!!)
        Assertions.assertEquals("2024-11-15T08:00:00Z", teamEvent.startDate.isoString())
        Assertions.assertEquals("2024-11-15T09:00:00Z", teamEvent.endDate.isoString())
        Assertions.assertEquals("2024-11-01T11:00:00Z", teamEvent.dtStamp.isoString())
    }

    /** RFC 5545 3.6.1: without DTEND the end is start + DURATION, else one day (all-day) or the start itself. */
    @Test
    fun `the end of an event without DTEND is derived`() {
        fun parse(start: String, duration: String? = null): TeamEventDO {
            val ics = listOfNotNull("BEGIN:VEVENT", "DTSTART$start", duration?.let { "DURATION:$it" }, "END:VEVENT")
                .joinToString("\n")
            return VEventUtils.convertToEventDO(VEventUtils.parseVEventFromIcs(ics)!!)
        }
        parse(":20241115T090000Z", "PT1H30M").let {
            Assertions.assertEquals("2024-11-15T10:30:00Z", it.endDate.isoString())
        }
        parse(";VALUE=DATE:20241115", "P2D").let {
            Assertions.assertTrue(it.allDay)
            Assertions.assertEquals("2024-11-17T00:00:00Z", it.endDate.isoString())
        }
        parse(";VALUE=DATE:20241115").let {
            Assertions.assertEquals("2024-11-16T00:00:00Z", it.endDate.isoString())
        }
        // An hour on a DATE can't be added: treated as missing, i.e. one day.
        parse(";VALUE=DATE:20241115", "PT1H").let {
            Assertions.assertEquals("2024-11-16T00:00:00Z", it.endDate.isoString())
        }
        parse(":20241115T090000Z").let {
            Assertions.assertEquals("2024-11-15T09:00:00Z", it.endDate.isoString())
        }
    }

    @Test
    fun `test of writing ics`() {
        // Europe/Berlin
        var teamEvent = TeamEventDO().apply {
            subject = "Team Meeting"
            note = "Discuss quarterly goals."
            location = "Conference Room"
            startDate = PFDateTime.withDate(2024, Month.NOVEMBER, 14, 9, 0).utilDate
            endDate = PFDateTime.withDate(2024, Month.NOVEMBER, 15, 9, 0).utilDate
        }
        val vEvent = VEventUtils.convertToVEvent(teamEvent)
        teamEvent = VEventUtils.convertToEventDO(vEvent)
        Assertions.assertNotNull(teamEvent)
        Assertions.assertEquals("Team Meeting", teamEvent.subject)
        Assertions.assertEquals("Conference Room", teamEvent.location)
        Assertions.assertEquals("Discuss quarterly goals.", teamEvent.note)
        Assertions.assertEquals("2024-11-14T08:00:00Z", teamEvent.startDate.isoString())
        Assertions.assertEquals("2024-11-15T08:00:00Z", teamEvent.endDate.isoString())
    }

    private val testIcs = """
    BEGIN:VEVENT
    SUMMARY:Team Meeting
    DTSTART:20241115T090000Z
    DTEND:20241115T100000Z
    DTSTAMP:20241101T120000Z
    LOCATION:Conference Room
    DESCRIPTION:Discuss quarterly goals.
    END:VEVENT
    """.trimIndent()

    // Relaxed parsing accepts a DTSTAMP with TZID (seen in iCloud/Google subscriptions):
    private val testIcsWithTimeZone = """
    BEGIN:VEVENT
    SUMMARY:Team Meeting
    DTSTART;TZID=Europe/Berlin:20241115T090000
    DTEND;TZID=Europe/Berlin:20241115T100000
    DTSTAMP;TZID=Europe/Berlin:20241101T120000
    END:VEVENT
    """.trimIndent()

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestSetup.init()
        }
    }
}
