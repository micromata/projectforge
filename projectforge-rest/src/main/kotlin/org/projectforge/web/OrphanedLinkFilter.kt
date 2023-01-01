/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.business.vacation.service.VacationSendMailService
import java.io.IOException
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

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
        val uri = servletRequest.requestURI
        // /wa/wicket/bookmarkable/org.projectforge.web.vacation.VacationEditPage?id=26422747
        if (uri.contains("/wa/wicket/bookmarkable/org.projectforge.web.vacation.VacationEditPage")) {
            servletResponse as HttpServletResponse
            val redirectUrl = VacationSendMailService.getLinkToVacationEntry(servletRequest.getParameter("id"))
            log.info("Redirect orphaned link '$uri' to '$redirectUrl'.")
            servletResponse.sendRedirect(redirectUrl)
            return
        }
        chain.doFilter(servletRequest, servletResponse)
    }

    /**
     * NOP.
     * @see Filter.init
     */
    @Throws(ServletException::class)
    override fun init(fConfig: FilterConfig) {
    }
}
