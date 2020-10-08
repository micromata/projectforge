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

package org.projectforge.rest.config

import mu.KotlinLogging
import org.projectforge.common.StringHelper
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.core.SessionCsrfCache
import org.projectforge.ui.ValidationError
import javax.servlet.Filter
import javax.servlet.FilterRegistration
import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

object RestUtils {
    @JvmStatic
    fun registerFilter(sc: ServletContext, name: String, filterClass: Class<out Filter?>, isMatchAfter: Boolean, vararg patterns: String?): FilterRegistration {
        val filterRegistration: FilterRegistration = sc.addFilter(name, filterClass)
        filterRegistration.addMappingForUrlPatterns(null, isMatchAfter, *patterns)
        log.info("Registering filter '" + name + "' of class '" + filterClass.name + "' for urls: " + StringHelper.listToString(", ", *patterns))
        return filterRegistration
    }

    /**
     * Checks the CSRF token. If the user is logged in by an authenticationToken [RestAuthenticationInfo.loggedInByAuthenticationToken] and the CSRF token is missed no check will be done.
     * Therefore pure Rest clients may not care about the CSRF token.
     */
    @JvmStatic
    fun checkCsrfToken(request: HttpServletRequest, sessionCsrfCache: SessionCsrfCache, csrfToken: String?, logInfo: String, logData: Any?): ValidationError? {
        if (csrfToken.isNullOrBlank() && ThreadLocalUserContext.getUserContext()?.loggedInByAuthenticationToken == true) {
            if (log.isDebugEnabled) {
                log.debug { "User '${ThreadLocalUserContext.getUser()?.username}' logged in by rest call, not by session." }
            }
            return null
        }
        if (!sessionCsrfCache.checkToken(request, csrfToken)) {
            log.warn("Check of CSRF token failed, a validation error will be shown. $logInfo declined: ${logData}")
            return ValidationError.create("errorpage.csrfError")
        }
        return null
    }
}
