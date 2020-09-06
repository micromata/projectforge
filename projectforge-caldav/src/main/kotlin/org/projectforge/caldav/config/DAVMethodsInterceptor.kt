/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.caldav.config

import mu.KotlinLogging
import org.projectforge.business.user.UserGroupCache
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val METHODS = arrayOf("OPTIONS", "PROPPATCH", "REPORT")

private val log = KotlinLogging.logger {}

/**
 * Enables http methods, required by DAV functionality such as "PROPFIND" etc.
 */
class DAVMethodsInterceptor : HandlerInterceptorAdapter() {
    /**
     * Needed to add support for DAV methods, such as "PROPFIND" etc. Otherwise Spring reject such http methods.
     */
    override fun preHandle(request: HttpServletRequest,
                           response: HttpServletResponse, handler: Any): Boolean {
        return !handledByMiltonFilter(request) // Support PROPFIND, PROPPATCH etc.
    }

    companion object {
        /**
         * @return true if [PFMiltonInit.available] and given method is in list "PROPFIND", "PROPPATCH", "OPTIONS" (with /users/...), "REPORT".
         * Otherwise false.
         */
        internal fun handledByMiltonFilter(request: HttpServletRequest): Boolean {
            if (!PFMiltonInit.available) {
                return false
            }
            val method = request.method
            if (method == "PROPFIND") {
                val uri = request.requestURI
                log.info("PROPFIND call detected: $uri")
                // All PROPFIND's will be handled by Milton.
                return true
            }
            if (METHODS.contains(method)) {
                val uri = request.requestURI
                // We may add uri pattern later ("/", "/principals/*", "/users/*").
                return uri.matches("/+users.*".toRegex())
            }
            return false
        }
    }
}
