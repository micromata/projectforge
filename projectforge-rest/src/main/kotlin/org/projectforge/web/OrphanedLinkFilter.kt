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
import mu.KotlinLogging
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
        } else if (redirectGoneReactPage(servletResponse, uri)) {
            // Handled: a link to an old React page of a category whose way back is Wicket was redirected.
        } else if (redirectMigratedPage(servletRequest, servletResponse, uri)) {
            // Handled: a link to a legacy page that has moved to projectforge-next was redirected.
        } else {
            chain.doFilter(servletRequest, servletResponse)
        }
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
     * Bends a bookmarked or emailed link to a legacy list/edit/add page that has moved to
     * projectforge-next onto its new url (see [NextMigration.orphanedLinks]).
     *
     * The escape hatch - the "way back" link projectforge-next shows on a migrated page - points at the
     * very same legacy urls, so it carries [NextMigration.ESCAPE_HATCH_PARAM] to be let through instead
     * of being bounced straight back to next.
     *
     * @return true if the request was a migrated legacy link and a redirect was sent.
     */
    private fun redirectMigratedPage(
        request: HttpServletRequest,
        response: ServletResponse,
        uri: String,
    ): Boolean {
        if (request.getParameter(NextMigration.ESCAPE_HATCH_PARAM) != null) {
            return false // The escape hatch: let it reach the legacy page.
        }
        for (link in NextMigration.orphanedLinks()) {
            // The edit page first: its path (e.g. wa/orderBookEdit) is more specific than the list path,
            // and for the React app the list path is even a prefix of it (react/group vs react/group/edit).
            if (uri.contains("/${link.legacyEditPath}")) {
                val id = when (link.legacyApp) {
                    NextMigration.LegacyApp.WICKET -> request.getParameter("id")?.toLongOrNull()
                    // react/group/edit/<id>; no id means the add page.
                    NextMigration.LegacyApp.REACT ->
                        uri.substringAfter("/${link.legacyEditPath}/", "").substringBefore('/').toLongOrNull()
                }
                val target = if (id != null) {
                    link.nextEditUrl.replace(NextMigration.ID_PLACEHOLDER, "$id")
                } else if (link.legacyApp == NextMigration.LegacyApp.WICKET && !request.queryString.isNullOrBlank()) {
                    // The presets of a Wicket add page travel as parameters (e.g. wa/ganttEdit?task=42 of the
                    // task page) and mean the same on the next new-entry page (newEntryParams).
                    "${link.nextNewEntryUrl}?${request.queryString}"
                } else {
                    link.nextNewEntryUrl
                }
                redirect(response, uri, target)
                return true
            }
            if (uri.contains("/${link.legacyListPath}")) {
                redirect(response, uri, link.nextListUrl)
                return true
            }
        }
        return false
    }

    private fun redirect(servletResponse: ServletResponse, uri: String, redirectUrl: String) {
        servletResponse as HttpServletResponse
        log.info("Redirect orphaned link '$uri' to '$redirectUrl'.")
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

        /** Categories migrated from Wicket whose old React pages are gone, see [redirectGoneReactPage]. */
        private val GONE_REACT_CATEGORIES = listOf("project", "task")
    }
}
