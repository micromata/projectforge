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
     * The links of pages that never left ProjectForge (no mail, no data transfer) are not redirected, see
     * NextMigration.NextPage.legacyApp: the old React pages are left to the React app, the Wicket ones land on the
     * next start page as any other Wicket url.
     */
    @Test
    fun `links that never left ProjectForge are not redirected`() {
        val categories = listOf(
            "project", "task", "teamCal", "user", "contract", "visitorbook", "address", "addressLegacy",
            "addressCampaignValue", "addressCampaignValueLegacy", "datatransfer", "merlin",
        )
        for (category in categories) {
            Assertions.assertNull(redirectOf("/react/$category"), category)
            Assertions.assertNull(redirectOf("/react/$category/edit/42"), category)
        }
        for (page in listOf(
            "addressView", "myAccount", "tokenInfo", "2FASetup", "WebAuthnSetup", "2FA", "changeWlanPassword", "pollInfo",
            "vacationAccount", "vacationExport", "costSearch", "customizeMenu", "birthdayButler", "eInvoiceChecker",
            "sipgate", "jobsMonitor", "systemStatistics", "merlinexecution",
        )) {
            Assertions.assertNull(redirectOf("/react/$page/dynamic"), page)
        }
        Assertions.assertNull(redirectOf("/react/group"))
        for (page in listOf(
            "search", "personalStatistics", "feedback", "monthlyEmployeeReport", "hrList", "datevImport", "admin",
            "userPrefList", "taskTree", "groupList", "groupEdit", "oldCalendar", "oldTeamCalendar",
            "wicket/bookmarkable/org.projectforge.plugins.ihk.IHKPage",
            "wicket/bookmarkable/org.projectforge.web.admin.TaskWizardPage",
            "wicket/bookmarkable/org.projectforge.web.teamcal.event.TeamEventEditPage",
            "wicket/bookmarkable/org.projectforge.web.teamcal.admin.TeamCalListPage",
        )) {
            Assertions.assertEquals("/next/", redirectOf("/wa/$page", mapOf("id" to "42")), page)
        }
        Assertions.assertEquals("/next/", redirectOf("/wa/accessList"))
        Assertions.assertEquals("/next/", redirectOf("/wa/timesheetList", mapOf("taskId" to "42")))
        Assertions.assertEquals("/next/", redirectOf("/wa/incomingInvoiceEdit", mapOf("id" to "42")))
    }

    /**
     * Renamed next pages linked by the error digest mail: the former route leads to the new one, its query carried
     * over. Neither the new route nor siblings or the client router's payloads are caught, nor renamed pages that
     * were never linked from outside.
     */
    @Test
    fun `renamed next pages are redirected to their new route`() {
        Assertions.assertEquals("/next/systemDashboard", redirectOf("/next/adminErrors/"))
        Assertions.assertEquals("/next/systemDashboard?id=42", redirectOf("/next/adminErrors", mapOf("id" to "42")))
        Assertions.assertEquals("/next/systemDashboard", redirectOf("/next/problemDashboard"))
        Assertions.assertEquals("/next/systemDashboard?id=42", redirectOf("/next/problemDashboard", mapOf("id" to "42")))
        Assertions.assertNull(redirectOf("/next/orderStatistics"))
        Assertions.assertNull(redirectOf("/next/systemStatistics"))
        Assertions.assertNull(redirectOf("/next/adminLogViewer"))
        Assertions.assertNull(redirectOf("/next/systemDashboard"))
        Assertions.assertNull(redirectOf("/next/problemDashboardXyz"))
        Assertions.assertNull(redirectOf("/next/adminErrors.txt"))
        Assertions.assertNull(redirectOf("/rs/adminErrors/list"))
    }

    /** The old Wicket calendars, the former page after login, still lead to the next calendar. */
    @Test
    fun `the old wicket calendar is redirected to next`() {
        val web = "/wa/wicket/bookmarkable/org.projectforge.web"
        Assertions.assertEquals("/next/calendar", redirectOf("/wa/calendar"))
        Assertions.assertEquals("/next/calendar", redirectOf("/wa/teamCalendar"))
        Assertions.assertEquals("/next/calendar", redirectOf("$web.calendar.CalendarPage"))
        Assertions.assertEquals("/next/calendar", redirectOf("$web.teamcal.integration.TeamCalCalendarPage"))
    }

    /** The old Wicket login page, the former start page. */
    @Test
    fun `the old wicket login page is redirected to the start page`() {
        Assertions.assertEquals("/", redirectOf("/wa/login"))
    }

    /**
     * The dynamic React pages of the data transfer plugin are gone; the links of the notification mails (file
     * view, -1 = own personal box) are bent onto the next pages.
     */
    @Test
    fun `the old react data transfer pages are redirected to next`() {
        Assertions.assertEquals("/next/datatransfer/42", redirectOf("/react/datatransferfiles/dynamic/42"))
        Assertions.assertEquals("/next/datatransfer/-1", redirectOf("/react/datatransferfiles/dynamic/-1"))
        Assertions.assertEquals("/next/datatransfer/42", redirectOf("/react/datatransferaudit/dynamic/42"))
        Assertions.assertEquals("/next/datatransfer/personal-box", redirectOf("/react/datatransferpersonalfiles/dynamic"))
        Assertions.assertEquals("/next/datatransfer", redirectOf("/react/datatransferfiles/dynamic/foo"))
    }

    /**
     * The React external access of a data transfer area is gone; the links sent to external users before are
     * bent onto the next external access. Only alphanumeric tokens get into the Location header.
     */
    @Test
    fun `the old react external access of data transfer is redirected to next`() {
        Assertions.assertEquals(
            "/next/public/datatransfer/abc123/",
            redirectOf("/react/public/datatransfer/dynamic/abc123"),
        )
        Assertions.assertEquals(
            "/next/public/datatransfer/abc123/",
            redirectOf("/react/public/datatransfer/dynamic", mapOf("id" to "abc123")),
        )
        Assertions.assertEquals("/next/", redirectOf("/react/public/datatransfer/dynamic/a%22b"))
        Assertions.assertEquals("/next/", redirectOf("/react/public/datatransfer/dynamic", mapOf("id" to "a/b")))
        Assertions.assertEquals("/next/", redirectOf("/react/public/datatransfer"))
    }

    /** The Wicket page of the IHK plugin has moved to projectforge-next; there is no way back to it. */
    /**
     * The React vacation list and form are gone: bookmarked links and those of old notification mails are bent onto
     * the next pages. The vacation account and the vacation export, whose React routes start with the same name,
     * are other pages with their own rules.
     */
    @Test
    fun `the old react vacation pages are redirected to next`() {
        Assertions.assertEquals("/next/vacation", redirectOf("/react/vacation"))
        Assertions.assertEquals("/next/vacation/42", redirectOf("/react/vacation/edit/42"))
        Assertions.assertEquals("/next/vacation/42", redirectOf("/react/vacation/edit/42", mapOf("returnToCaller" to "account")))
        Assertions.assertEquals("/next/vacation/new", redirectOf("/react/vacation/edit"))
        Assertions.assertNotEquals("/next/vacation", redirectOf("/react/vacationAccount/dynamic"))
        Assertions.assertNotEquals("/next/vacation", redirectOf("/react/vacationExport/dynamic"))
    }

    /**
     * The old React page for changing the password (linked by the "password not changed" mails of 9.0.5) is gone;
     * only a numeric user id is passed on.
     */
    @Test
    fun `the old react change password page is redirected to next`() {
        Assertions.assertEquals("/next/changePassword", redirectOf("/react/changePassword/dynamic"))
        Assertions.assertEquals("/next/changePassword", redirectOf("/react/changePassword"))
        Assertions.assertEquals(
            "/next/changePassword?userId=42",
            redirectOf("/react/changePassword/dynamic", mapOf("userId" to "42")),
        )
        Assertions.assertEquals(
            "/next/changePassword",
            redirectOf("/react/changePassword/dynamic", mapOf("userId" to "42&foo=bar")),
        )
        Assertions.assertNull(redirectOf("/next/changePassword"))
        Assertions.assertNull(redirectOf("/rs/changePassword/init"))
    }

    /**
     * The old React answer page of a poll (linked by its mails) is gone; only a numeric poll id is passed on. The list and form of the polls were never linked from outside and are not redirected.
     */
    @Test
    fun `the old react poll answer page is redirected to next`() {
        Assertions.assertEquals(
            "/next/poll/42/answer",
            redirectOf("/react/pollResponse/dynamic/", mapOf("pollId" to "42")),
        )
        Assertions.assertEquals("/next/poll", redirectOf("/react/pollResponse/dynamic", mapOf("pollId" to "4x")))
        // The reminder mails of a poll ending soon (8.0, 8.1) linked react/response/dynamic/<pollId>.
        Assertions.assertEquals("/next/poll/42/answer", redirectOf("/react/response/dynamic/42"))
        Assertions.assertEquals("/next/poll", redirectOf("/react/response/dynamic/4x"))
        Assertions.assertNull(redirectOf("/react/responseXyz"))
        Assertions.assertNull(redirectOf("/rs/pollAnswer/42"))
    }

    /**
     * Wicket is no way back any more: a Wicket url is redirected even with the escape marker (a bookmarked
     * "classic version" link of a page migrated from Wicket), while a React one is still let through.
     */
    @Test
    fun `a wicket page is redirected to next even with the escape marker`() {
        Assertions.assertEquals("/next/order", redirectOf("/wa/orderBookList", NextMigration.ESCAPE_HATCH_PARAM))
        Assertions.assertEquals(
            "/next/order/5",
            redirectOf("/wa/orderBookEdit", mapOf("id" to "5", NextMigration.ESCAPE_HATCH_PARAM to "")),
        )
        Assertions.assertEquals(
            "/next/todo/7",
            redirectOf("/wa/wicket/bookmarkable/org.projectforge.plugins.todo.ToDoEditPage", mapOf("id" to "7")),
        )
        Assertions.assertNull(redirectOf("/react/vacation", NextMigration.ESCAPE_HATCH_PARAM))
    }

    /** The Wicket direct call, meant to be called by url, keeps its deep link parameters on next. */
    @Test
    fun `the old wicket direct call is redirected to next`() {
        Assertions.assertEquals("/next/phoneCall", redirectOf("/wa/phoneCall"))
        Assertions.assertEquals(
            "/next/phoneCall?addressId=7&number=0123",
            redirectOf("/wa/phoneCall", mapOf("addressId" to "7", "number" to "0123")),
        )
        Assertions.assertEquals(
            "/next/phoneCall?addressId=7&number=0123&callerPage=addressView",
            redirectOf("/wa/phoneCall", mapOf("address" to "7", "no" to "0123", "cp" to "addressView")),
        )
        Assertions.assertEquals("/next/", redirectOf("/wa/phoneCallXyz"), "Not the direct call: the catch-all.")
    }

    /** Every other Wicket url lands on the next start page: no request reaches Wicket any more. */
    @Test
    fun `any other wicket url is redirected to the next start page`() {
        Assertions.assertEquals("/next/", redirectOf("/wa"))
        Assertions.assertEquals("/next/", redirectOf("/wa/anything"))
        Assertions.assertEquals("/next/", redirectOf("/wa/wicket/page", mapOf("3" to "")))
        // A React url of no migrated page (a category no page has) is left alone:
        Assertions.assertNull(redirectOf("/react/notMigratedCategory"))
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
