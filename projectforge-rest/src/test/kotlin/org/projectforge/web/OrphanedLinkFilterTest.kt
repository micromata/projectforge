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

package org.projectforge.web

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.NextMigration
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class OrphanedLinkFilterTest {
    private val filter = OrphanedLinkFilter()

    /**
     * The React calendar is a migrated page like any other now: a bookmarked or emailed link to it is bent
     * onto the next calendar. Next offers no way back to it any more (`offerLegacyLink = false`).
     */
    @Test
    fun `the react calendar is redirected to next`() {
        Assertions.assertEquals("/next/calendar", redirectOf("/react/calendar"))
    }

    /**
     * The escape hatch: a request carrying [NextMigration.ESCAPE_HATCH_PARAM] is let through to the legacy
     * page, which is how a "classic version" link reaches it without being bounced back. The calendar serves
     * as the example; next itself no longer links there.
     */
    @Test
    fun `the react calendar with the escape marker stays in the legacy app`() {
        Assertions.assertNull(
            redirectOf("/react/calendar", NextMigration.ESCAPE_HATCH_PARAM),
            "A request with the escape marker must stay in React.",
        )
    }

    /**
     * The exemption is scoped to the calendar subtree: another migrated React page's bookmarked link is
     * still bent onto next (the group page, migrated from the React app).
     */
    @Test
    fun `a migrated react page outside the calendar is still redirected`() {
        Assertions.assertEquals("/next/group", redirectOf("/react/group"))
    }

    /**
     * The old React project list is gone, although the project's legacy app is Wicket: a bookmarked link to
     * it, list or form, is bent onto next. A sibling path isn't caught.
     */
    @Test
    fun `the old react project pages are redirected to next`() {
        Assertions.assertEquals("/next/project", redirectOf("/react/project"))
        Assertions.assertEquals("/next/project", redirectOf("/react/project/"))
        Assertions.assertEquals("/next/project/42", redirectOf("/react/project/edit/42"))
        Assertions.assertEquals("/next/project/new", redirectOf("/react/project/edit"))
        Assertions.assertNull(redirectOf("/react/projectXyz"))
    }

    /** The same for the old React task pages: the task's way back is Wicket, its React pages are gone. */
    @Test
    fun `the old react task pages are redirected to next`() {
        Assertions.assertEquals("/next/task", redirectOf("/react/task"))
        Assertions.assertEquals("/next/task/42", redirectOf("/react/task/edit/42"))
        Assertions.assertEquals("/next/task/new", redirectOf("/react/task/edit"))
        Assertions.assertNull(redirectOf("/react/taskTree"))
    }

    /** Old Wicket calendars, bookmarked by some users, still lead to the next calendar. */
    @Test
    fun `the old wicket calendar is redirected to next`() {
        Assertions.assertEquals("/next/calendar", redirectOf("/wa/calendar"))
    }

    /**
     * The removed Wicket calendar pages: the old calendars and the team event list lead to the next calendar,
     * the event editor to next's, the team calendar administration to React and the ICS import to next — ids
     * and the import's calendar carried over, anything not numeric dropped.
     */
    @Test
    fun `the removed wicket calendar pages are redirected`() {
        val web = "/wa/wicket/bookmarkable/org.projectforge.web"
        Assertions.assertEquals("/next/calendar", redirectOf("/wa/oldCalendar"))
        Assertions.assertEquals("/next/calendar", redirectOf("/wa/oldTeamCalendar"))
        Assertions.assertEquals("/next/calendar", redirectOf("$web.teamcal.event.TeamEventListPage"))
        Assertions.assertEquals(
            "/next/teamEvent/42",
            redirectOf("$web.teamcal.event.TeamEventEditPage", mapOf("id" to "42")),
        )
        Assertions.assertEquals(
            "/next/teamEvent/new",
            redirectOf("$web.teamcal.event.TeamEventEditPage", mapOf("id" to "42&x=y")),
        )
        Assertions.assertEquals("/react/teamCal", redirectOf("$web.teamcal.admin.TeamCalListPage"))
        Assertions.assertEquals(
            "/react/teamCal/edit/7",
            redirectOf("$web.teamcal.admin.TeamCalEditPage", mapOf("id" to "7")),
        )
        Assertions.assertEquals("/react/teamCal/edit", redirectOf("$web.teamcal.admin.TeamCalEditPage"))
        Assertions.assertEquals(
            "/next/teamCalImport?teamCalId=7",
            redirectOf("$web.teamcal.event.importics.TeamCalImportPage", mapOf("teamCalId" to "7")),
        )
        Assertions.assertEquals(
            "/next/teamCalImport",
            redirectOf("$web.teamcal.event.importics.TeamCalImportPage"),
        )
        Assertions.assertEquals("/next/", redirectOf("/wa/oldCalendarXyz"), "Not a calendar page: the catch-all.")
    }

    /** The Wicket global search has moved to projectforge-next; a bookmarked link is bent onto it. */
    @Test
    fun `the old wicket search is redirected to next`() {
        Assertions.assertEquals("/next/search", redirectOf("/wa/search"))
    }

    /** The Wicket feedback page has moved to projectforge-next; a bookmarked or emailed link is bent onto it. */
    @Test
    fun `the old wicket feedback page is redirected to next`() {
        Assertions.assertEquals("/next/feedback", redirectOf("/wa/feedback"))
    }

    /** The Wicket monthly employee report has moved to projectforge-next; a bookmarked link is bent onto it. */
    @Test
    fun `the old wicket monthly employee report is redirected to next`() {
        Assertions.assertEquals(
            "/next/monthlyEmployeeReport",
            redirectOf("/wa/monthlyEmployeeReport"),
        )
    }

    /** The Wicket report was removed, so the escape marker no longer lets a request through to it. */
    @Test
    fun `the old wicket monthly employee report is redirected to next even with the escape marker`() {
        Assertions.assertEquals(
            "/next/monthlyEmployeeReport",
            redirectOf("/wa/monthlyEmployeeReport", NextMigration.ESCAPE_HATCH_PARAM),
        )
    }

    /** The Wicket page of the IHK plugin has moved to projectforge-next; there is no way back to it. */
    @Test
    fun `the old wicket IHK page is redirected to next`() {
        val uri = "/wa/wicket/bookmarkable/org.projectforge.plugins.ihk.IHKPage"
        Assertions.assertEquals("/next/ihk", redirectOf(uri))
        Assertions.assertEquals("/next/ihk", redirectOf(uri, NextMigration.ESCAPE_HATCH_PARAM))
    }

    /** The Wicket HR view has moved to projectforge-next; a bookmarked link is bent onto it, a sibling isn't. */
    @Test
    fun `the old wicket HR view is redirected to next`() {
        Assertions.assertEquals("/next/hrList", redirectOf("/wa/hrList"))
        Assertions.assertEquals("/next/hrList", redirectOf("/wa/hrList", NextMigration.ESCAPE_HATCH_PARAM))
        Assertions.assertEquals("/next/", redirectOf("/wa/hrListXyz"), "Not the HR view: the catch-all.")
    }

    @Test
    fun `the old wicket DATEV import is redirected to next`() {
        Assertions.assertEquals("/next/datev-import", redirectOf("/wa/datevImport"))
        Assertions.assertEquals("/next/datev-import", redirectOf("/wa/datevImport", NextMigration.ESCAPE_HATCH_PARAM))
        Assertions.assertEquals("/next/", redirectOf("/wa/datevImportXyz"), "Not the DATEV import: the catch-all.")
    }

    /**
     * The Wicket System (administration) page has moved to projectforge-next and is gone, so the escape marker
     * no longer lets a request through. Sibling pages like /wa/adminLogViewer aren't caught (only by the catch-all).
     */
    @Test
    fun `the old wicket admin page is redirected to next`() {
        Assertions.assertEquals("/next/system", redirectOf("/wa/admin"))
        Assertions.assertEquals("/next/system", redirectOf("/wa/admin", NextMigration.ESCAPE_HATCH_PARAM))
        Assertions.assertEquals("/next/", redirectOf("/wa/adminLogViewer"), "Not the System page: the catch-all.")
    }

    /**
     * The Wicket user preference pages are gone without a replacement, so their links land on the next start page.
     */
    @Test
    fun `the old wicket user preference pages are redirected to next`() {
        Assertions.assertEquals("/next/", redirectOf("/wa/userPrefList"))
        Assertions.assertEquals("/next/", redirectOf("/wa/userPrefEdit"))
        Assertions.assertEquals("/next/", redirectOf("/wa/userPrefEdit", NextMigration.ESCAPE_HATCH_PARAM))
        Assertions.assertEquals("/next/", redirectOf("/wa/userPrefListXyz"))
    }

    /**
     * Wicket is no way back any more: a Wicket url is redirected even with the escape marker (a bookmarked
     * "classic version" link of a page migrated from Wicket), while a React one is still let through.
     */
    @Test
    fun `a wicket page is redirected to next even with the escape marker`() {
        Assertions.assertEquals("/next/access", redirectOf("/wa/accessList", NextMigration.ESCAPE_HATCH_PARAM))
        Assertions.assertEquals(
            "/next/access?taskId=42",
            redirectOf("/wa/accessList", mapOf("taskId" to "42", NextMigration.ESCAPE_HATCH_PARAM to "")),
            "The task preset of the list is carried over, the escape marker is dropped.",
        )
        Assertions.assertEquals("/next/gantt/new?task=7", redirectOf("/wa/ganttEdit", mapOf("task" to "7")))
        Assertions.assertEquals(
            "/next/task/5",
            redirectOf("/wa/taskEdit", mapOf("id" to "5", NextMigration.ESCAPE_HATCH_PARAM to "")),
        )
        Assertions.assertEquals("/next/account", redirectOf("/wa/accountList"))
        Assertions.assertEquals("/next/accounting-record/3", redirectOf("/wa/accountingRecordEdit", mapOf("id" to "3")))
        Assertions.assertEquals("/next/gantt", redirectOf("/wa/ganttList"))
        Assertions.assertNull(redirectOf("/react/group", NextMigration.ESCAPE_HATCH_PARAM))
    }

    /** The last Wicket pages outside NextMigration's conventions are bent onto their next successors. */
    @Test
    fun `the last wicket pages are redirected to next`() {
        Assertions.assertEquals("/next/taskTree", redirectOf("/wa/taskTree"))
        Assertions.assertEquals("/next/taskTree?highlightId=42", redirectOf("/wa/taskTree", mapOf("row" to "42")))
        Assertions.assertEquals(
            "/next/taskWizard",
            redirectOf("/wa/wicket/bookmarkable/org.projectforge.web.admin.TaskWizardPage"),
        )
        Assertions.assertEquals("/next/phoneCall", redirectOf("/wa/phoneCall"))
        Assertions.assertEquals(
            "/next/phoneCall?addressId=7&number=0123",
            redirectOf("/wa/phoneCall", mapOf("addressId" to "7", "number" to "0123")),
        )
        Assertions.assertEquals(
            "/next/phoneCall?addressId=7&number=0123&callerPage=addressView",
            redirectOf("/wa/phoneCall", mapOf("address" to "7", "no" to "0123", "cp" to "addressView")),
        )
        Assertions.assertEquals("/next/timesheet", redirectOf("/wa/timesheetList"))
        Assertions.assertEquals("/next/timesheet/42", redirectOf("/wa/timesheetEdit", mapOf("id" to "42")))
        Assertions.assertEquals("/next/timesheet/new", redirectOf("/wa/timesheetEdit"))
        Assertions.assertEquals("/next/timesheet/new", redirectOf("/wa/timesheetEdit", mapOf("id" to "42&x=y")))
        Assertions.assertEquals("/next/group", redirectOf("/wa/groupList"))
        Assertions.assertEquals("/next/group/8", redirectOf("/wa/groupEdit", mapOf("id" to "8")))
    }

    /** Every other Wicket url lands on the next start page: no request reaches Wicket any more. */
    @Test
    fun `any other wicket url is redirected to the next start page`() {
        Assertions.assertEquals("/next/", redirectOf("/wa"))
        Assertions.assertEquals("/next/", redirectOf("/wa/anything"))
        Assertions.assertEquals("/next/", redirectOf("/wa/wicket/page", mapOf("3" to "")))
        Assertions.assertNull(redirectOf("/react/address"))
        Assertions.assertNull(redirectOf("/next/task"))
        Assertions.assertNull(redirectOf("/water"))
    }

    /**
     * Runs the filter over a GET of [uri] and returns the redirect location it sent, or null if it let the
     * request pass through to the chain untouched. Each of [params] is added as a valueless query parameter,
     * so the escape-hatch marker can be exercised.
     */
    private fun redirectOf(uri: String, vararg params: String): String? =
        redirectOf(uri, params.associateWith { "" })

    /** As [redirectOf], with [params] carrying values. */
    private fun redirectOf(uri: String, params: Map<String, String>): String? {
        val request = MockHttpServletRequest("GET", uri).also { it.requestURI = uri }
        params.forEach { (name, value) -> request.addParameter(name, value) }
        if (params.isNotEmpty()) {
            request.queryString = params.entries.joinToString("&") { (name, value) ->
                if (value.isEmpty()) name else "$name=$value"
            }
        }
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, MockFilterChain())
        return response.redirectedUrl
    }
}
