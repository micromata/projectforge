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
            redirect(servletResponse, uri, "/react/calendar")
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
        } else if (redirectMigratedPage(servletRequest, servletResponse, uri)) {
            // Handled: a link to a legacy page that has moved to projectforge-next was redirected.
        } else {
            chain.doFilter(servletRequest, servletResponse)
        }
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
    }
}
