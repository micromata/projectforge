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
        if (uri.contains("/wa/login")) { // Old Wicket login page, bookmarked by some users.
            redirect(servletResponse, uri, "/")
        } else if (uri.contains("/wa/calendar") || uri.contains("/wa/teamCalendar")) { // Old Wicket calendars, bookmarked by some users.
            redirect(servletResponse, uri, "/${NextMigration.listUrl("calendar")}")
        } else if (uri.contains("/wa/search")) { // Old Wicket global search, migrated to projectforge-next.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}search")
        } else if (uri.contains("/wa/personalStatistics")) { // Old Wicket personal statistics, migrated to
            // projectforge-next. The Wicket page was removed, so there is no "classic version" escape hatch.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}personalStatistics")
        } else if (uri.contains("/wa/feedback")) { // Old Wicket feedback page, migrated to projectforge-next.
            // The Wicket page was removed, so there is no "classic version" escape hatch.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}feedback")
        } else if (uri.contains("/wa/monthlyEmployeeReport")) { // Old Wicket monthly report, migrated to
            // projectforge-next. The Wicket page was removed, so there is no "classic version" escape hatch.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}monthlyEmployeeReport")
        } else if (uri.contains("/wa/wicket/bookmarkable/org.projectforge.plugins.ihk.IHKPage")) { // Old Wicket
            // IHK report of the IHK plugin, migrated to projectforge-next. The Wicket page was removed, so there is
            // no "classic version" escape hatch.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}ihk")
        } else if (uri.endsWith("/wa/hrList") || uri.contains("/wa/hrList/")) { // Old Wicket HR view, migrated to
            // projectforge-next. The Wicket page was removed, so there is no "classic version" escape hatch. The
            // precise segment match keeps this from catching sibling pages.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}hrList")
        } else if (uri.endsWith("/wa/datevImport") || uri.contains("/wa/datevImport/")) { // Old Wicket DATEV import,
            // migrated to projectforge-next. The Wicket page was removed, so there is no "classic version" escape hatch.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}datev-import")
        } else if (uri.endsWith("/wa/admin") || uri.contains("/wa/admin/")) {
            // Old Wicket System (administration) page, migrated to projectforge-next. The Wicket page was removed,
            // so there is no "classic version" escape hatch. The precise segment match keeps this from catching
            // sibling pages like /wa/adminLogViewer.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}system")
        } else if (uri.endsWith("/react/systemStatistics") || uri.contains("/react/systemStatistics/")) {
            // Old dynamic React page of the system statistics, migrated to projectforge-next. The React page was
            // removed, so there is no "classic version" escape hatch.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}$SYSTEM_STATISTICS_ROUTE")
        } else if (uri.endsWith("/react/myAccount") || uri.contains("/react/myAccount/")
            || uri.endsWith("/react/tokenInfo") || uri.contains("/react/tokenInfo/")
        ) {
            // Old dynamic React pages of my account and of a token's usage (a modal of my account), migrated to
            // projectforge-next. The React pages were removed, so there is no "classic version" escape hatch. The
            // precise segment match keeps this from catching sibling pages like /react/myScript.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}myAccount")
        } else if (uri.endsWith("/react/2FASetup") || uri.contains("/react/2FASetup/")
            || uri.endsWith("/react/WebAuthnSetup") || uri.contains("/react/WebAuthnSetup/")
        ) {
            // Old dynamic React pages of the 2FA setup and of a WebAuthn key (a dialog of the setup now), migrated to
            // projectforge-next. The React pages were removed, so there is no "classic version" escape hatch. The
            // precise segment match keeps this from catching the 2FA page /react/2FA. No parameters are passed on.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}2FASetup")
        } else if (uri.endsWith("/react/changePassword") || uri.contains("/react/changePassword/")
            || uri.endsWith("/react/changeWlanPassword") || uri.contains("/react/changeWlanPassword/")
        ) {
            // Old dynamic React pages for changing the (WLAN) password, migrated to projectforge-next. The React
            // pages were removed, so there is no "classic version" escape hatch. The user id (admins changing
            // another user's password) is interpolated into the Location header, so only a number is passed on.
            val page = if (uri.contains("/react/changeWlanPassword")) "changeWlanPassword" else "changePassword"
            val userId = servletRequest.getParameter("userId")?.toLongOrNull()
            val query = if (userId != null) "?userId=$userId" else ""
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}$page$query")
        } else if (uri.endsWith("/react/pollResponse") || uri.contains("/react/pollResponse/")
            || uri.endsWith("/react/pollInfo") || uri.contains("/react/pollInfo/")
        ) {
            // Old dynamic React pages of the polls: the answer page (linked by the mails of a poll) and the guide,
            // migrated to projectforge-next. The React pages were removed, so there is no "classic version" escape
            // hatch. The poll id is interpolated into the Location header, so only a number is passed on.
            val pollId = servletRequest.getParameter("pollId")?.toLongOrNull()
            val target = if (uri.contains("/react/pollResponse") && pollId != null) "poll/$pollId/answer" else "poll"
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}$target")
        } else if (uri.endsWith("/react/vacationAccount") || uri.contains("/react/vacationAccount/")) {
            // Old dynamic React page of the vacation account, migrated to projectforge-next. The React page was
            // removed, so there is no "classic version" escape hatch. The employee (HR staff, as path segment
            // react/vacationAccount/dynamic/<id> or as `?id=`) is interpolated into the Location header, so only a
            // number is passed on.
            val employeeId = uri.substringAfter("/react/vacationAccount/dynamic/", "").substringBefore('/')
                .toLongOrNull() ?: servletRequest.getParameter("id")?.toLongOrNull()
            val query = if (employeeId != null) "?employeeId=$employeeId" else ""
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}vacationAccount$query")
        } else if (uri.endsWith("/react/costSearch") || uri.contains("/react/costSearch/")) {
            // Old dynamic React page of the cost search, migrated to projectforge-next (removed, so no "classic
            // version" escape hatch). It took no parameters.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}costSearch")
        } else if (uri.endsWith("/react/customizeMenu") || uri.contains("/react/customizeMenu/")) {
            // Old React menu customizer, migrated to projectforge-next (removed, so no "classic version" escape
            // hatch). It took no parameters. Also reached by favorites menus still holding the old url.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}customizeMenu")
        } else if (uri.endsWith("/react/birthdayButler") || uri.contains("/react/birthdayButler/")) {
            // Old dynamic React page of the birthday butler, migrated to projectforge-next (removed, so no
            // "classic version" escape hatch). It took no parameters.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}birthdayButler")
        } else if (uri.endsWith("/react/eInvoiceChecker") || uri.contains("/react/eInvoiceChecker/")) {
            // Old dynamic React page of the e-invoice checker, migrated to projectforge-next (removed, so no
            // "classic version" escape hatch). It took no parameters.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}eInvoiceChecker")
        } else if (uri.endsWith("/react/vacationExport") || uri.contains("/react/vacationExport/")) {
            // Old dynamic React page of the team vacation export, migrated to projectforge-next (removed, so no
            // "classic version" escape hatch). It took no parameters.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}vacationExport")
        } else if (uri.endsWith("/wa/userPrefList") || uri.contains("/wa/userPrefList/")
            || uri.endsWith("/wa/userPrefEdit") || uri.contains("/wa/userPrefEdit/")
        ) {
            // Old Wicket user preferences (favorites/templates), removed without a replacement page: the task
            // favorites and the timesheet templates are managed where they are used. Lands on the next start page.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}")
        } else if (uri.contains("/wa/wicket/bookmarkable/org.projectforge.web.vacation.VacationEditPage")) {
            // /wa/wicket/bookmarkable/org.projectforge.web.vacation.VacationEditPage?id=26422747
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
        } else if (redirectRemovedCalendarPage(servletRequest, servletResponse, uri)) {
            // Handled: a link to a removed Wicket calendar page was redirected.
        } else if (redirectGoneReactPage(servletResponse, uri)) {
            // Handled: a link to an old React page of a category whose way back is Wicket was redirected.
        } else if (redirectAddressPage(servletRequest, servletResponse, uri)) {
            // Handled: a link to the React view or import page of the addresses was redirected.
        } else if (redirectDataTransferPage(servletRequest, servletResponse, uri)) {
            // Handled: a link to a dynamic React page of the data transfer plugin was redirected.
        } else if (redirectPublicDataTransferPage(servletRequest, servletResponse, uri)) {
            // Handled: a link to the React external access of a data transfer area was redirected.
        } else if (redirectScriptExecutePage(servletRequest, servletResponse, uri)) {
            // Handled: a link to a React script execution page was redirected.
        } else if (redirectMigratedPage(servletRequest, servletResponse, uri)) {
            // Handled: a link to a legacy page that has moved to projectforge-next was redirected.
        } else if (redirectLastWicketPage(servletRequest, servletResponse, uri)) {
            // Handled: a link to one of the last Wicket pages outside NextMigration's conventions was redirected.
        } else if (uri.endsWith("/wa") || uri.contains("/wa/")) {
            // Catch-all: no request reaches Wicket any more. Every old link not mapped above (and any Wicket
            // internal url, e.g. wa/wicket/page?3) lands on the next start page instead of an error page.
            redirect(servletResponse, uri, "/${Constants.NEXT_APP_PATH}")
        } else {
            chain.doFilter(servletRequest, servletResponse)
        }
    }

    /**
     * The pages of projectforge-next that moved to another route ([RENAMED_NEXT_PAGES]), e.g. bookmarked or
     * linked by an error digest mail (`next/problemDashboard?id=42`). The query (the problem's id, the tab) means the
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
     * The removed Wicket calendar pages: the old calendars (wa/oldCalendar, wa/oldTeamCalendar), the team event
     * list (dropped, the events are reached through the calendar) and edit page, the team calendar
     * administration and the ICS import (both next). All but the old calendars were bookmarkable pages only
     * (wa/wicket/bookmarkable/<class>), taking their id as the parameter `id` (the import: `teamCalId`). Ids are
     * interpolated into the Location header, so only numbers are accepted.
     *
     * @return true if the request was such a link and a redirect was sent.
     */
    private fun redirectRemovedCalendarPage(
        request: HttpServletRequest,
        response: ServletResponse,
        uri: String,
    ): Boolean {
        val id = request.getParameter("id")?.toLongOrNull()
        val target = when {
            uri.endsWith("/wa/oldCalendar") || uri.endsWith("/wa/oldTeamCalendar")
                || uri.contains("$BOOKMARKABLE.calendar.CalendarPage")
                || uri.contains("$BOOKMARKABLE.teamcal.integration.TeamCalCalendarPage")
                || uri.contains("$BOOKMARKABLE.teamcal.event.TeamEventListPage") ->
                NextMigration.listUrl("calendar")

            uri.contains("$BOOKMARKABLE.teamcal.event.TeamEventEditPage") ->
                if (id != null) {
                    NextMigration.nextEditPage("teamEvent")!!.replace(NextMigration.ID_PLACEHOLDER, "$id")
                } else {
                    NextMigration.newEntryUrl("teamEvent")
                }

            uri.contains("$BOOKMARKABLE.teamcal.admin.TeamCalListPage") -> NextMigration.listUrl("teamCal")
            uri.contains("$BOOKMARKABLE.teamcal.admin.TeamCalEditPage") ->
                if (id != null) {
                    NextMigration.standardEditPage("teamCal").replace(NextMigration.ID_PLACEHOLDER, "$id")
                } else {
                    NextMigration.newEntryUrl("teamCal")
                }

            uri.contains("$BOOKMARKABLE.teamcal.event.importics.TeamCalImportPage") -> {
                val teamCalId = request.getParameter("teamCalId")?.toLongOrNull()
                "${Constants.NEXT_APP_PATH}teamCalImport" + (teamCalId?.let { "?teamCalId=$it" } ?: "")
            }

            else -> return false
        }
        redirect(response, uri, "/$target")
        return true
    }

    /**
     * The old React pages (list and form) of [GONE_REACT_CATEGORIES], migrated to projectforge-next. Not
     * covered by [redirectMigratedPage]: their legacy app is Wicket, the way back. The React pages are gone
     * (their EntityRest serves no layout), so there is no escape hatch to let through. The precise segment
     * match keeps this from catching sibling pages like react/projectXyz or react/taskTree.
     * react/<category>/edit/<id>; no id means the add page.
     *
     * @return true if the request was such a link and a redirect was sent.
     */
    private fun redirectGoneReactPage(response: ServletResponse, uri: String): Boolean {
        val category = GONE_REACT_CATEGORIES.find { uri.endsWith("/react/$it") || uri.contains("/react/$it/") }
            ?: return false
        val isEdit = uri.endsWith("/react/$category/edit") || uri.contains("/react/$category/edit/")
        val id = uri.substringAfter("/react/$category/edit/", "").substringBefore('/').toLongOrNull()
        val target = when {
            id != null -> NextMigration.nextEditPage(category)!!.replace(NextMigration.ID_PLACEHOLDER, "$id")
            isEdit -> NextMigration.newEntryUrl(category)
            else -> NextMigration.listUrl(category)
        }
        redirect(response, uri, "/$target")
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
     * The React script execution pages, migrated to projectforge-next: react/scriptExecute/dynamic/<id> (a
     * stored script, without an id the ad-hoc editor, `?example=<n>` with an example script) and
     * react/myScriptExecute/dynamic/<id>. The lists and the form are covered by [redirectMigratedPage] - which
     * must come after this, because react/script is a prefix of these urls. The pages are gone, so there is
     * no escape hatch. Id and example are interpolated into the Location header, so only numbers are
     * accepted; the React app took the id as path segment or as parameter.
     *
     * @return true if the request was such a link and a redirect was sent.
     */
    private fun redirectScriptExecutePage(
        request: HttpServletRequest,
        response: ServletResponse,
        uri: String,
    ): Boolean {
        val (page, category) = SCRIPT_EXECUTE_PAGES.entries
            .find { (page, _) -> uri.contains("/react/$page/") || uri.endsWith("/react/$page") }
            ?.toPair() ?: return false
        val listUrl = NextMigration.listUrl(category)
        val id = uri.substringAfter("/react/$page/dynamic/", "").substringBefore('/').toLongOrNull()
            ?: request.getParameter("id")?.toLongOrNull()
        val target = when {
            id != null -> "$listUrl/$id"
            category == "script" ->
                "$listUrl/execute" + (request.getParameter("example")?.toIntOrNull()?.let { "?example=$it" } ?: "")
            else -> listUrl
        }
        redirect(response, uri, "/$target")
        return true
    }

    /**
     * The dynamic React pages of the addresses, which have no category of their own in [NextMigration] (the list
     * and the form are bent by [redirectMigratedPage]): the view `react/addressView/dynamic/<id>` (also `?id=`, and
     * `react/calendar/addressView/...` as the React calendar opened it for a birthday) leads to the view of
     * projectforge-next, the VCF import `react/addressImportUpload/dynamic` to its import page. Checked before
     * [redirectMigratedPage], whose `calendar` entry would take the calendar variant for a calendar link. Ids are
     * interpolated into the Location header, so only numbers are accepted.
     *
     * @return true if the request was such a link and a redirect was sent.
     */
    private fun redirectAddressPage(
        request: HttpServletRequest,
        response: ServletResponse,
        uri: String,
    ): Boolean {
        val listUrl = "/${NextMigration.listUrl("address")}"
        if (uri.endsWith("/react/addressImportUpload") || uri.contains("/react/addressImportUpload/")) {
            redirect(response, uri, "$listUrl/import")
            return true
        }
        val page = listOf("react/calendar/addressView", "react/addressView").find { containsPath(uri, it) }
            ?: return false
        val id = uri.substringAfter("/$page/dynamic/", "").substringBefore('/').toLongOrNull()
            ?: request.getParameter("id")?.toLongOrNull()
        val target = when {
            id == null -> listUrl
            page.startsWith("react/calendar/") -> "$listUrl/$id?returnTo=/calendar"
            else -> "$listUrl/$id"
        }
        redirect(response, uri, target)
        return true
    }

    /**
     * Bends a bookmarked or emailed link to a legacy list/edit/add page that has moved to
     * projectforge-next onto its new url (see [NextMigration.orphanedLinks]).
     *
     * The escape hatch - the "way back" link projectforge-next shows on a page migrated from React - points
     * at the very same legacy urls, so it carries [NextMigration.ESCAPE_HATCH_PARAM] to be let through
     * instead of being bounced straight back to next. Wicket is no way back any more: a Wicket url is
     * redirected even with the marker (e.g. a bookmarked escape hatch link).
     *
     * @return true if the request was a migrated legacy link and a redirect was sent.
     */
    private fun redirectMigratedPage(
        request: HttpServletRequest,
        response: ServletResponse,
        uri: String,
    ): Boolean {
        val escapeHatch = request.getParameter(NextMigration.ESCAPE_HATCH_PARAM) != null
        for (link in NextMigration.orphanedLinks()) {
            if (escapeHatch && link.legacyApp == NextMigration.LegacyApp.REACT) {
                continue // The escape hatch: let it reach the legacy React page.
            }
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
     * The Wicket pages still mounted until the module is deleted that [redirectMigratedPage] doesn't cover:
     * the task tree, the direct call and the task wizard (no category of their own), and the list/edit pages
     * of [WICKET_PAGES_OF_REACT_CATEGORIES], whose legacy app in [NextMigration] is React. The precise
     * segment match keeps this from catching sibling pages. Ids are interpolated into the Location header,
     * so only numbers are accepted.
     *
     * @return true if the request was such a link and a redirect was sent.
     */
    private fun redirectLastWicketPage(
        request: HttpServletRequest,
        response: ServletResponse,
        uri: String,
    ): Boolean {
        val target = when {
            // Wicket's highlighted row (AbstractListPage.PARAMETER_HIGHLIGHTED_ROW) is next's highlightId.
            isWicketPage(uri, "taskTree") ->
                "${Constants.NEXT_APP_PATH}taskTree${wicketQuery(request, mapOf("row" to "highlightId"))}"
            uri.contains("$BOOKMARKABLE.admin.TaskWizardPage") -> "${Constants.NEXT_APP_PATH}taskWizard"
            // The deep link parameters (addressId, number, callerPage) mean the same on the next page; Wicket also
            // accepted the short aliases address, no and cp.
            isWicketPage(uri, "phoneCall") -> "${Constants.NEXT_APP_PATH}phoneCall" +
                    wicketQuery(request, mapOf("address" to "addressId", "no" to "number", "cp" to "callerPage"))

            else -> {
                val category = WICKET_PAGES_OF_REACT_CATEGORIES.find {
                    isWicketPage(uri, "${it}List") || isWicketPage(uri, "${it}Edit")
                } ?: return false
                val id = request.getParameter("id")?.toLongOrNull()
                when {
                    isWicketPage(uri, "${category}List") -> NextMigration.listUrl(category) + listQuery(request, category)
                    id != null -> NextMigration.nextEditPage(category)!!.replace(NextMigration.ID_PLACEHOLDER, "$id")
                    else -> NextMigration.newEntryUrl(category)
                }
            }
        }
        redirect(response, uri, "/$target")
        return true
    }

    /**
     * The filter of a Wicket list link that next's list understands under the same name: the time sheet list's
     * `taskId`, `userId` and `kost2Id` (TimesheetListPage's parameters, read by next's timesheet page). Only
     * numbers are carried, as they end up in the Location header.
     *
     * @return e.g. `?taskId=42&userId=7`, or an empty string.
     */
    private fun listQuery(request: HttpServletRequest, category: String): String {
        if (category != "timesheet") return ""
        val query = TIMESHEET_LIST_PARAMS.mapNotNull { name ->
            request.getParameter(name)?.toLongOrNull()?.let { "$name=$it" }
        }
        return if (query.isEmpty()) "" else "?${query.joinToString("&")}"
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

    /** Whether [uri] is the Wicket mount point [mountPoint] (e.g. `/wa/taskTree`, `/wa/taskTree/...`). */
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

        /** Former route -> current route of renamed next pages (without `next/`), see [redirectRenamedNextPage]. */
        private val RENAMED_NEXT_PAGES = mapOf(
            "orderStatistics" to "finance/statistics",
            "adminErrors" to "systemDashboard",
            "problemDashboard" to "systemDashboard",
            "systemStatistics" to SYSTEM_STATISTICS_ROUTE,
            // The admin log viewer, a tab of the system dashboard; its query (?source=, ?search=, ?incident=) is kept.
            "adminLogViewer" to "systemDashboard?tab=logs",
        )

        /** The system statistics, a tab of the system dashboard. */
        private const val SYSTEM_STATISTICS_ROUTE = "systemDashboard?tab=statistics"

        /** Categories migrated from Wicket whose old React pages are gone, see [redirectGoneReactPage]. */
        private val GONE_REACT_CATEGORIES = listOf("project", "task")

        /** The dynamic React pages of the data transfer plugin, see [redirectDataTransferPage]. */
        private val DATATRANSFER_DYNAMIC_PAGES =
            listOf("datatransferfiles", "datatransferaudit", "datatransferpersonalfiles")

        /** The external access token of a data transfer area, see [redirectPublicDataTransferPage]. */
        private val ACCESS_TOKEN_REGEX = Regex("[A-Za-z0-9]+")

        /** The React script execution pages and their category, see [redirectScriptExecutePage]. */
        private val SCRIPT_EXECUTE_PAGES = mapOf("scriptExecute" to "script", "myScriptExecute" to "myscript")

        /**
         * Categories migrated from React whose Wicket list/edit pages (`wa/<category>List`, `wa/<category>Edit`)
         * are still mounted, see [redirectLastWicketPage].
         */
        private val WICKET_PAGES_OF_REACT_CATEGORIES = listOf("timesheet", "group")

        /** The filter parameters of Wicket's time sheet list that next's list reads under the same name. */
        private val TIMESHEET_LIST_PARAMS = listOf("taskId", "userId", "kost2Id")
    }
}
