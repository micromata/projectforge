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

package org.projectforge.rest.calendar.importer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.importer.AbstractImportPageRest
import org.projectforge.rest.importer.ImportEntry
import org.projectforge.rest.importer.ImportView
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockMultipartFile

class TeamEventImportRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    @Autowired
    private lateinit var teamEventDao: TeamEventDao

    @Autowired
    private lateinit var teamEventImportRest: TeamEventImportRest

    @Test
    fun `events are imported, updated by uid and ambiguous uids refused`() {
        val owner = logon(TEST_USER)
        val calId = createCalendar("ICS import test", owner)
        val otherCalId = createCalendar("ICS import other", owner)
        val request = newRequest()

        // Upload into the calendar: both events are new.
        var view = upload(request, ics(event("uid-1", "Kick-off"), event("uid-2", "Review")), calId)
        assertEquals(listOf(ImportEntry.Status.NEW, ImportEntry.Status.NEW), statuses(view))
        assertEquals(calId, view.meta?.get("teamCalId"))
        commitAll(request)
        assertEquals("Kick-off", teamEventDao.getByUid(calId, "uid-1")?.subject)
        assertEquals("Review", teamEventDao.getByUid(calId, "uid-2")?.subject)

        // Upload again with one changed subject: one modified, one unmodified, and the update keeps the event.
        val storedId = teamEventDao.getByUid(calId, "uid-1")!!.id
        view = upload(request, ics(event("uid-1", "Kick-off moved"), event("uid-2", "Review")), calId)
        assertEquals(listOf(ImportEntry.Status.MODIFIED, ImportEntry.Status.UNMODIFIED), statuses(view))
        commitAll(request)
        val updated = teamEventDao.getByUid(calId, "uid-1")!!
        assertEquals(storedId, updated.id)
        assertEquals("Kick-off moved", updated.subject)

        // The same uids are new in another calendar.
        view = teamEventImportRest.target(request, otherCalId).body as ImportView<*>
        assertEquals(listOf(ImportEntry.Status.NEW, ImportEntry.Status.NEW), statuses(view))

        // A uid twice in the file is faulty.
        view = upload(request, ics(event("uid-3", "A"), event("uid-3", "B")), calId)
        assertEquals(listOf(ImportEntry.Status.FAULTY, ImportEntry.Status.FAULTY), statuses(view))
    }

    @Test
    fun `only writable calendars are accepted as target`() {
        val owner = logon(TEST_USER2)
        val foreignCalId = createCalendar("ICS import foreign", owner)
        val user = logon(TEST_USER)
        val subscriptionCalId = createCalendar("ICS import subscription", user) {
            it.externalSubscription = true
            it.externalSubscriptionUrl = "http://localhost:9/calendar.ics" // Nobody listens: fails at once.
        }
        val request = newRequest()
        listOf(foreignCalId, subscriptionCalId).forEach { calId ->
            val view = upload(request, ics(event("uid-x", "X")), calId)
            assertEquals(null, view.meta?.get("teamCalId"), "Calendar #$calId must not be a target.")
            assertEquals(listOf(ImportEntry.Status.UNKNOWN), statuses(view), "Not reconciled without a target.")
            assertEquals(400, teamEventImportRest.target(request, calId).statusCode.value())
        }
    }

    private fun createCalendar(title: String, owner: PFUserDO, init: (TeamCalDO) -> Unit = {}): Long {
        return teamCalDao.insert(TeamCalDO().also {
            it.title = title
            it.owner = owner
            init(it)
        })
    }

    private fun newRequest() = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }

    private fun upload(request: MockHttpServletRequest, content: String, teamCalId: Long): ImportView<*> {
        request.setParameter("teamCalId", teamCalId.toString())
        val file = MockMultipartFile("file", "events.ics", "text/calendar", content.toByteArray())
        val response = teamEventImportRest.upload(request, file)
        assertEquals(200, response.statusCode.value(), "${response.body}")
        return response.body as ImportView<*>
    }

    /** Runs the import job of all importable entries directly, without the job handler's coroutine. */
    private fun commitAll(request: MockHttpServletRequest) {
        val storage = ExpiringSessionAttributes.getAttribute(
            request,
            AbstractImportPageRest.getSessionAttributeName(TeamEventImportRest::class.java),
        ) as TeamEventImportStorage
        val selected = storage.pairEntries.filter {
            it.status == ImportEntry.Status.NEW || it.status == ImportEntry.Status.MODIFIED
        }
        assertTrue(selected.isNotEmpty())
        TeamEventImportJob(teamCalDao, teamEventDao, selected, storage).importSelected()
    }

    private fun statuses(view: ImportView<*>) = view.entries.map { it.status }

    private fun event(uid: String, summary: String) = """
        BEGIN:VEVENT
        UID:$uid
        DTSTAMP:20260101T080000Z
        DTSTART:20260105T090000Z
        DTEND:20260105T100000Z
        SUMMARY:$summary
        END:VEVENT
    """.trimIndent()

    private fun ics(vararg events: String) =
        "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//ProjectForge//Test//EN\n${events.joinToString("\n")}\nEND:VCALENDAR\n"
}
