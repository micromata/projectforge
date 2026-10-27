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

import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.Constants
import org.projectforge.NextMigration
import org.projectforge.business.vacation.service.VacationSendMailService
import org.projectforge.menu.builder.MenuItemDefId
import java.io.IOException

private val log = KotlinLogging.logger {}

/*
 * Redirect orphaned links from former versions of ProjectForge (e. g. if link in e-mails were changed due to migrations or refactoring.
 * Only links that left ProjectForge (sent by mail or shared by a data transfer area) get a redirect of their own, and the
 * former start pages (login, calendar), which browsers offer and users keep; any other old Wicket or React link lands on
 * the next start page (catch-all), as neither frontend exists any more.
 */
class OrphanedLinkFilter : Filter {
    /**
     * NOP.
     * @see Filter.destroy
     */
    override fun destroy() {}

    /**
     * @see Filter.doFilter
     */
    @Throws(IOException::class, ServletException::class)
    override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, chain: FilterChain) {
        if (servletRequest !is HttpServletRequest) {
            // Not for us.
            chain.doFilter(servletRequest, servletResponse)
            return
        }
        val uri = servletRequest.requestURI ?: ""
        if (uri.contains("/wa/login")) { // Old Wicket login page, the former start page.
            redirect(servletResponse, uri, "/")
        } else if (uri.contains("/wa/calendar") || uri.contains("/wa/teamCalendar")) { // Old Wicket calendars, the
            // former page after login.
            redirect(servletResponse, uri, "/${NextMigration.listUrl("calendar")}")
        } else if (uri.endsWith("/react/changePassword") || uri.contains("/react/changePassword/")) {
            // Old dynamic React page for changing the password, linked by the "password not changed" mails of 9.0.5
            // (react/changePassword/dynamic) and migrated to projectforge-next. The React page was removed, so there is
            // no "classic version" escape hatch. The user id (admins changing another user's password) is
            // interpolated into the Location header, so only a number is passed on.
            val userId = servletRequest.getParameter("userId")?.toLongOrNull()
            val query = if (userId != null) "?userId=$userId" else ""
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}changePassword$query")
        } else if (uri.endsWith("/react/pollResponse") || uri.contains("/react/pollResponse/")) {
            // Old dynamic React page answering a poll, linked by the mails of a poll and migrated to
            // projectforge-next. The React page was removed, so there is no "classic version" escape hatch. The
            // poll id is interpolated into the Location header, so only a number is passed on.
            val pollId = servletRequest.getParameter("pollId")?.toLongOrNull()
            val target = if (pollId != null) "poll/$pollId/answer" else "poll"
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}$target")
        } else if (uri.endsWith("/react/response") || uri.contains("/react/response/")) {
            // The answer page as the reminder mails of a poll ending soon linked it (8.0 and 8.1, PollCronJobs):
            // react/response/dynamic/<pollId>. Only a number is passed on, as above.
            val pollId = uri.substringAfter("/react/response/dynamic/", "").substringBefore('/').toLongOrNull()
            val target = if (pollId != null) "poll/$pollId/answer" else "poll"
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}$target")
        } else if (uri.contains("/wa/wicket/bookmarkable/org.projectforge.web.vacation.VacationEditPage")) {
            // /wa/wicket/bookmarkable/org.projectforge.web.vacation.VacationEditPage?id=4711
            // The id is interpolated into the Location header, so only accept what an id can be: anything else is
            // either a broken bookmark or somebody trying to smuggle query params into the target url.
            val id = servletRequest.getParameter("id")?.toLongOrNull()
            if (id == null) {
                log.info { "Orphaned link '$uri' without a valid id parameter, redirecting to the vacation list." }
                redirect(servletResponse, uri, VACATION_LIST_URL)
            } else {
                redirect(servletResponse, uri, VacationSendMailService.getLinkToVacationEntry(id))
            }
        } else if (redirectRenamedNextPage(servletRequest, servletResponse, uri)) {
            // Handled: a link to a page of projectforge-next that has moved to another route was redirected.
        } else if (redirectRemovedCalendarPage(servletResponse, uri)) {
            // Handled: a link to a removed Wicket calendar page was redirected.
        } else if (redirectDataTransferPage(servletRequest, servletResponse, uri)) {
            // Handled: a link to a dynamic React page of the data transfer plugin was redirected.
        } else if (redirectPublicDataTransferPage(servletRequest, servletResponse, uri)) {
            // Handled: a link to the React external access of a data transfer area was redirected.
        } else if (redirectMigratedPage(servletRequest, servletResponse, uri)) {
            // Handled: a link to a legacy page that has moved to projectforge-next was redirected.
        } else if (isWicketPage(uri, "phoneCall")) {
            // The Wicket direct call, the one page meant to be called by url from outside: its deep link parameters
            // (addressId, number, callerPage) mean the same on the next page; Wicket also accepted the short aliases
            // address, no and cp.
            val query = wicketQuery(servletRequest, mapOf("address" to "addressId", "no" to "number", "cp" to "callerPage"))
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}phoneCall$query")
        } else if (uri.endsWith("/wa") || uri.contains("/wa/") || uri.endsWith("/react") || uri.contains("/react/")) {
            // Catch-all: the Wicket and the React app are gone. Every old link not mapped above (and any Wicket
            // internal url, e.g. wa/wicket/page?3) lands on the next start page instead of an error page.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}")
        } else {
            chain.doFilter(servletRequest, servletResponse)
        }
    }

    /**
     * The pages of projectforge-next that moved to another route and were linked by the error digest mail
     * ([RENAMED_NEXT_PAGES], e.g. `next/problemDashboard?id=42`). The query (the problem's id, the tab) means the
     * same on the new route and is carried over. The precise segment match keeps this from catching sibling
     * pages and the payloads (`.txt`) of the client router.
     *
     * @return true if the request was such a link and a redirect was sent.
     */
    private fun redirectRenamedNextPage(
        request: HttpServletRequest,
        response: ServletResponse,
        uri: String,
    ): Boolean {
        val newRoute = RENAMED_NEXT_PAGES.entries.find { (oldRoute, _) ->
            val path = "/${Constants.NEXT_APP_PATH}$oldRoute"
            uri.endsWith(path) || uri.contains("$path/")
        }?.value ?: return false
        // A new route with a query of its own (a tab) takes the request's query behind it.
        val query = wicketQuery(request).let { if (it.isNotEmpty() && newRoute.contains('?')) "&${it.drop(1)}" else it }
        redirect(response, uri, "/${Constants.NEXT_APP_PATH}$newRoute$query")
        return true
    }

    /**
     * The removed Wicket calendars as the former page after login (bookmarkable pages only,
     * wa/wicket/bookmarkable/<class>) lead to the next calendar.
     *
     * @return true if the request was such a link and a redirect was sent.
     */
    private fun redirectRemovedCalendarPage(response: ServletResponse, uri: String): Boolean {
        if (!uri.contains("$BOOKMARKABLE.calendar.CalendarPage")
            && !uri.contains("$BOOKMARKABLE.teamcal.integration.TeamCalCalendarPage")
        ) {
            return false
        }
        redirect(response, uri, "/${NextMigration.listUrl("calendar")}")
        return true
    }

    /**
     * The dynamic React pages of the data transfer plugin, migrated to projectforge-next: the file view of an
     * area (react/datatransferfiles/dynamic/<id>, linked by the notification mails, -1 is the own personal
     * box), its activities (react/datatransferaudit/dynamic/<id>, now a dialog of the file view) and the
     * personal box of another user (react/datatransferpersonalfiles/dynamic). The list and the admin form are
     * covered by [redirectMigratedPage] - which must come after this, because react/datatransfer is a prefix
     * of these urls. The pages are gone, so there is no escape hatch. The id is interpolated into the
     * Location header, so only numbers are accepted; the React app took it as path segment or as parameter.
     *
     * @return true if the request was such a link and a redirect was sent.
     */
    private fun redirectDataTransferPage(
        request: HttpServletRequest,
        response: ServletResponse,
        uri: String,
    ): Boolean {
        val listUrl = NextMigration.listUrl("datatransfer")
        val page = DATATRANSFER_DYNAMIC_PAGES.find { uri.contains("/react/$it/") || uri.endsWith("/react/$it") }
            ?: return false
        val target = if (page == "datatransferpersonalfiles") {
            "$listUrl/personal-box"
        } else {
            val id = uri.substringAfter("/react/$page/dynamic/", "").substringBefore('/').toLongOrNull()
                ?: request.getParameter("id")?.toLongOrNull()
            if (id != null) "$listUrl/$id" else listUrl
        }
        redirect(response, uri, "/$target")
        return true
    }

    /**
     * The React external access of a data transfer area (react/public/datatransfer/dynamic/<token>, the link of
     * the area sent to external users before), migrated to projectforge-next (next/public/datatransfer/<token>).
     * The page is gone, so there is no escape hatch. The token is interpolated into the Location header, so only
     * alphanumeric tokens are accepted (as generated); the React app took it as path segment or as parameter.
     * Without a valid token, the next start page.
     *
     * @return true if the request was such a link and a redirect was sent.
     */
    private fun redirectPublicDataTransferPage(
        request: HttpServletRequest,
        response: ServletResponse,
        uri: String,
    ): Boolean {
        val path = "/react/public/datatransfer"
        if (!uri.endsWith(path) && !uri.contains("$path/")) {
            return false
        }
        val token = uri.substringAfter("$path/dynamic/", "").substringBefore('/').ifEmpty { null }
            ?: request.getParameter("id")
        val target = if (token != null && ACCESS_TOKEN_REGEX.matches(token)) {
            "${Constants.NEXT_APP_PATH}public/datatransfer/$token/"
        } else {
            Constants.NEXT_APP_PATH
        }
        redirect(response, uri, "/$target")
        return true
    }

    /**
     * Bends a bookmarked or emailed link to a legacy list/edit/add page that has moved to
     * projectforge-next onto its new url (see [NextMigration.orphanedLinks]).
     *
     * A bookmarked escape hatch link (the former "way back" link, marked with [NextMigration.ESCAPE_HATCH_PARAM])
     * is redirected as well: neither Wicket nor React is left to go back to.
     *
     * @return true if the request was a migrated legacy link and a redirect was sent.
     */
    private fun redirectMigratedPage(
        request: HttpServletRequest,
        response: ServletResponse,
        uri: String,
    ): Boolean {
        for (link in NextMigration.orphanedLinks()) {
            // The edit page first: its path (e.g. wa/orderBookEdit) is more specific than the list path,
            // and for the React app the list path is even a prefix of it (react/group vs react/group/edit).
            if (containsPath(uri, link.legacyEditPath)) {
                val id = when (link.legacyApp) {
                    NextMigration.LegacyApp.WICKET -> request.getParameter("id")?.toLongOrNull()
                    // react/group/edit/<id>; no id means the add page.
                    NextMigration.LegacyApp.REACT ->
                        uri.substringAfter("/${link.legacyEditPath}/", "").substringBefore('/').toLongOrNull()
                }
                val target = if (id != null) {
                    link.nextEditUrl.replace(NextMigration.ID_PLACEHOLDER, "$id")
                } else if (link.legacyApp == NextMigration.LegacyApp.WICKET) {
                    // The presets of a Wicket add page travel as parameters (e.g. wa/ganttEdit?task=42 of the
                    // task page) and mean the same on the next new-entry page (newEntryParams).
                    "${link.nextNewEntryUrl}${wicketQuery(request)}"
                } else {
                    link.nextNewEntryUrl
                }
                redirect(response, uri, target)
                return true
            }
            if (containsPath(uri, link.legacyListPath)) {
                // The presets of a Wicket list travel as parameters too (wa/accessList?taskId=42 of the task page,
                // the same as next/access?taskId=42).
                val query = if (link.legacyApp == NextMigration.LegacyApp.WICKET) wicketQuery(request) else ""
                redirect(response, uri, "${link.nextListUrl}$query")
                return true
            }
        }
        return false
    }

    /**
     * Whether [uri] holds [path] as whole segments: `/react/vacation` and `/react/vacation/edit/42`, but not the
     * sibling pages `/react/vacationAccount/dynamic` or `/react/vacationExport/dynamic`, which a plain `contains`
     * would bend onto the vacation list.
     */
    private fun containsPath(uri: String, path: String): Boolean {
        val needle = "/$path"
        var index = uri.indexOf(needle)
        while (index >= 0) {
            val end = index + needle.length
            if (end == uri.length || uri[end] == '/' || uri[end] == ';') {
                return true
            }
            index = uri.indexOf(needle, index + 1)
        }
        return false
    }

    /**
     * The query string of a Wicket link to carry over onto next, without the escape hatch marker (meaningless
     * there).
     *
     * @param renames Wicket parameter name -> next parameter name, for the parameters named differently there.
     * @return e.g. `?task=42`, or an empty string.
     */
    private fun wicketQuery(request: HttpServletRequest, renames: Map<String, String> = emptyMap()): String {
        val query = request.queryString?.split('&')
            ?.filter { it.isNotBlank() && it.substringBefore('=') != NextMigration.ESCAPE_HATCH_PARAM }
            ?.joinToString("&") { param ->
                val name = param.substringBefore('=')
                renames[name]?.let { "$it${param.removePrefix(name)}" } ?: param
            }
        return if (query.isNullOrEmpty()) "" else "?$query"
    }

    /** Whether [uri] is the Wicket mount point [mountPoint] (e.g. `/wa/phoneCall`, `/wa/phoneCall/...`). */
    private fun isWicketPage(uri: String, mountPoint: String): Boolean {
        return uri.endsWith("/wa/$mountPoint") || uri.contains("/wa/$mountPoint/")
    }

    private fun redirect(servletResponse: ServletResponse, uri: String, redirectUrl: String) {
        servletResponse as HttpServletResponse
        log.info { "Redirect orphaned link '$uri' to '$redirectUrl'." }
        servletResponse.sendRedirect(redirectUrl)
        return
    }

    /**
     * NOP.
     * @see Filter.init
     */
    @Throws(ServletException::class)
    override fun init(fConfig: FilterConfig) {
    }

    companion object {
        private val VACATION_LIST_URL = MenuItemDefId.VACATION.url ?: "/"

        /** Prefix of the bookmarkable urls of the Wicket pages in org.projectforge.web. */
        private const val BOOKMARKABLE = "/wa/wicket/bookmarkable/org.projectforge.web"

        /**
         * Former route -> current route of renamed next pages (without `next/`) linked by the error digest mail
         * (LogGroupAdminService.DASHBOARD_PATH), see [redirectRenamedNextPage].
         */
        private val RENAMED_NEXT_PAGES = mapOf(
            "adminErrors" to "systemDashboard",
            "problemDashboard" to "systemDashboard",
        )

        /** The dynamic React pages of the data transfer plugin, see [redirectDataTransferPage]. */
        private val DATATRANSFER_DYNAMIC_PAGES =
            listOf("datatransferfiles", "datatransferaudit", "datatransferpersonalfiles")

        /** The external access token of a data transfer area, see [redirectPublicDataTransferPage]. */
        private val ACCESS_TOKEN_REGEX = Regex("[A-Za-z0-9]+")
    }
}
