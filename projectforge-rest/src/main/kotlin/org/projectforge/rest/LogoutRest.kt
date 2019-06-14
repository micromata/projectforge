/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest

import org.projectforge.business.user.UserXmlPreferencesCache
import org.projectforge.business.user.filter.CookieService
import org.projectforge.business.user.filter.UserFilter
import org.projectforge.rest.config.Rest
import org.projectforge.ui.UIStyle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * This rest service should be available without login (public).
 */
@RestController
@RequestMapping("${Rest.URL}/logout")
open class LogoutRest {
    private val log = org.slf4j.LoggerFactory.getLogger(LogoutRest::class.java)

    @Autowired
    private lateinit var cookieService: CookieService

    @Autowired
    private lateinit var userXmlPreferencesCache: UserXmlPreferencesCache

    @GetMapping
    fun logout(request: HttpServletRequest,
               response: HttpServletResponse)
            : ResponseData {
        val stayLoggedInCookie = cookieService.getStayLoggedInCookie(request)
        val user = UserFilter.getUser(request)
        if (user != null) {
            userXmlPreferencesCache.flushToDB(user.getId())
            userXmlPreferencesCache.clear(user.getId())
        }
        UserFilter.logout(request)
        if (stayLoggedInCookie != null) {
            stayLoggedInCookie.maxAge = 0
            stayLoggedInCookie.value = null
            stayLoggedInCookie.path = "/"
        }
        if (stayLoggedInCookie != null) {
            response.addCookie(stayLoggedInCookie)
        }
        return ResponseData("logout.successful", messageType = MessageType.TOAST, style = UIStyle.SUCCESS)
        //Response.temporaryRedirect(restHelper.buildUri(request, "login")).build()
    }
}
