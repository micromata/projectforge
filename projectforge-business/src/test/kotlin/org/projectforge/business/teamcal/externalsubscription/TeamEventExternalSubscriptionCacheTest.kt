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


package org.projectforge.business.teamcal.externalsubscription

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.business.teamcal.admin.model.TeamCalDO

class TeamEventExternalSubscriptionCacheTest {
    @Test
    fun `note is appended to the description`() {
        assertEquals("note", TeamEventExternalSubscriptionCache.appendNote(null, "note", 100))
        assertEquals("note", TeamEventExternalSubscriptionCache.appendNote("  ", "note", 100))
        assertEquals("Holidays\n\nnote", TeamEventExternalSubscriptionCache.appendNote("Holidays", "note", 100))
        // Too long: the description is shortened, the note is kept.
        assertEquals("abcd...\n\nnote", TeamEventExternalSubscriptionCache.appendNote("abcdefghijklmnop", "note", 13))
        assertEquals(13, TeamEventExternalSubscriptionCache.appendNote("abcdefghijklmnop", "note", 13).length)
        // Not even room for a shortened description:
        assertEquals("note", TeamEventExternalSubscriptionCache.appendNote("abcdefghijklmnop", "note", 8))
    }

    @Test
    fun `systemic failure`() {
        assertFalse(TeamEventExternalSubscriptionCache.isSystemicFailure(0, 0))
        assertFalse(TeamEventExternalSubscriptionCache.isSystemicFailure(2, 2), "Too few to be sure.")
        assertTrue(TeamEventExternalSubscriptionCache.isSystemicFailure(3, 3))
        assertTrue(TeamEventExternalSubscriptionCache.isSystemicFailure(3, 5))
        assertFalse(TeamEventExternalSubscriptionCache.isSystemicFailure(3, 6), "Half isn't more than half.")
        assertFalse(TeamEventExternalSubscriptionCache.isSystemicFailure(3, 20))
    }

    @Test
    fun `events of a failing subscription are loaded from the database`() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:test
            BEGIN:VEVENT
            UID:event-1
            DTSTAMP:20241101T120000Z
            DTSTART:20300101T080000Z
            DTEND:20300101T090000Z
            SUMMARY:Future event
            END:VEVENT
            END:VCALENDAR
        """.trimIndent().replace("\n", "\r\n")
        val calendar = TeamCalDO().also {
            it.id = 42
            it.externalSubscriptionCalendarBinary = ics.toByteArray()
        }
        val subscription = TeamEventSubscription()
        subscription.loadFromDatabase(calendar)
        assertTrue(subscription.isInitialized)
        assertNull(subscription.lastUpdated, "Loading from the database isn't an update.")
        assertEquals("Future event", subscription.getEvent("event-1")?.subject)

        // Without stored events:
        TeamEventSubscription().also {
            it.loadFromDatabase(TeamCalDO().also { cal -> cal.id = 43 })
            assertTrue(it.isInitialized)
            assertNull(it.getEvent("event-1"))
        }
    }
}
