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

package org.projectforge.rest.pub

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.projectforge.Constants
import org.projectforge.login.LoginService
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.ServerData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLDecoder
import java.net.URLEncoder

@Service
class LoginServiceRest {
    @Autowired
    private lateinit var loginService: LoginService

    fun logout(request: HttpServletRequest, response: HttpServletResponse): ResponseAction {
        val redirectUrl = getRedirectUrl(request, null).let {
            if (it.isNullOrBlank() || it.contains(PasswordForgottenPageRest.REST_PATH)) {
                // Don't redirect to password forgotten page:
                null
            } else {
                mapOf("url" to URLEncoder.encode(it, "UTF-8"))
            }
        }
        loginService.logout(request, response)
        return ResponseAction(
            PagesResolver.getDynamicPageUrl(
                LoginPageRest::
                class.java,
                absolute = true,
                params = redirectUrl,
            ), targetType = TargetType.CHECK_AUTHENTICATION
        )
    }

    companion object {
        private val ORIGIN_URL_SESSION_KEY = "${LoginPageRest::class.java.name}.originUrl"

        fun getRedirectUrl(request: HttpServletRequest, serverData: ServerData?): String? {
            var redirect: String? = null
            val returnToCaller =
                serverData?.returnToCaller ?: request.getSession(false)?.getAttribute(ORIGIN_URL_SESSION_KEY) as String?
            if (!returnToCaller.isNullOrBlank()) {
                redirect = URLDecoder.decode(returnToCaller, "UTF-8")
            } else if (request.getHeader("Referer")?.contains("/public/login") == true) {
                redirect = "/${Constants.REACT_APP_PATH}calendar"
            }
            // redirect might be "null" (string):
            return if (redirect.isNullOrBlank() || redirect == "null") null else redirect
        }

        internal fun storeOriginUrl(request: HttpServletRequest, url: String?) {
            request.getSession(false)?.setAttribute(ORIGIN_URL_SESSION_KEY, url)
        }
    }
}
