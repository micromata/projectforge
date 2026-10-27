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

package org.projectforge.gateway.compare

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class IcsNormalizerTest {
    private fun ics(vararg events: String) =
        "BEGIN:VCALENDAR\r\nPRODID:ProjectForge\r\n${events.joinToString("")}END:VCALENDAR\r\n"

    private fun event(uid: String, summary: String, dtStamp: String = "20261026T100000Z") =
        "BEGIN:VEVENT\r\nDTSTAMP:$dtStamp\r\nUID:$uid\r\nSUMMARY:$summary\r\nEND:VEVENT\r\n"

    @Test
    fun ignoresDtStampAndOrderOfEvents() {
        val main = ics(event("a", "Meeting"), event("b", "Workshop"))
        val gateway = ics(event("b", "Workshop", "20261026T120000Z"), event("a", "Meeting", "20261026T120001Z"))
        assertEquals(IcsNormalizer.normalize(main), IcsNormalizer.normalize(gateway))
        assertNotEquals(IcsNormalizer.normalize(main), IcsNormalizer.normalize(ics(event("a", "Meeting"))))
        assertEquals(2, IcsNormalizer.countEvents(main))
    }

    @Test
    fun unfoldsLines() {
        val folded = ics("BEGIN:VEVENT\r\nUID:a\r\nSUMMARY:A long\r\n  summary\r\nEND:VEVENT\r\n")
        val unfolded = ics("BEGIN:VEVENT\r\nUID:a\r\nSUMMARY:A long summary\r\nEND:VEVENT\r\n")
        assertEquals(IcsNormalizer.normalize(unfolded), IcsNormalizer.normalize(folded))
    }

    @Test
    fun keepsAlarmsWithinTheirEvent() {
        val withAlarm = ics("BEGIN:VEVENT\r\nUID:a\r\nBEGIN:VALARM\r\nTRIGGER:-PT15M\r\nEND:VALARM\r\nEND:VEVENT\r\n")
        assertEquals(
            "BEGIN:VCALENDAR\nPRODID:ProjectForge\nBEGIN:VEVENT\nUID:a\nBEGIN:VALARM\nTRIGGER:-PT15M\nEND:VALARM\nEND:VEVENT\nEND:VCALENDAR",
            IcsNormalizer.normalize(withAlarm),
        )
    }

    @Test
    fun neutralizesSetupEventOfEmptyCalendars() {
        fun setup(uid: String, start: String) =
            ics("BEGIN:VEVENT\r\nDTSTART;TZID=Europe/Berlin:$start\r\nSUMMARY:SETUP EVENT\r\nUID:$uid\r\nEND:VEVENT\r\n")
        assertEquals(
            IcsNormalizer.normalize(setup("0370b1a2", "20261026T131021")),
            IcsNormalizer.normalize(setup("4b3e1889", "20261026T131522")),
        )
    }
}
