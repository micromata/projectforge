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
import mu.KotlinLogging
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

private val log = KotlinLogging.logger {}

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
            val referer = request.getHeader("Referer")
            if (!returnToCaller.isNullOrBlank()) {
                // Sanitize after decoding: %2f%2fevil.com only becomes //evil.com here.
                redirect = sanitizeRedirectUrl(URLDecoder.decode(returnToCaller, "UTF-8"))
            } else if (referer?.contains("/${Constants.NEXT_APP_PATH}") == true) {
                // A login of projectforge-next should return to projectforge-next:
                redirect = "/${Constants.NEXT_APP_PATH}"
            } else if (referer?.contains("/public/login") == true) {
                redirect = "/${Constants.REACT_APP_PATH}calendar"
            }
            // redirect might be "null" (string):
            return if (redirect.isNullOrBlank() || redirect == "null") null else redirect
        }

        internal fun storeOriginUrl(request: HttpServletRequest, url: String?) {
            // Rejected here as well as in getRedirectUrl: a session might still carry an unchecked value from
            // a previous release, and serverData.returnToCaller never passes through here at all.
            request.getSession(false)?.setAttribute(ORIGIN_URL_SESSION_KEY, sanitizeRedirectUrl(url))
        }

        /**
         * After the login the user is sent to this url, so an attacker-supplied one would turn the login page
         * into an open redirect (and a convincing phishing hop: the victim really did log in to ProjectForge).
         * Only relative paths within this application are accepted - the same rule the client applies in
         * `projectforge-next/lib/menu-url.ts`, but the client's copy protects nobody.
         *
         * @return The url, or null if it isn't a relative path of this application.
         */
        internal fun sanitizeRedirectUrl(url: String?): String? {
            if (url.isNullOrBlank() || url == "null") {
                return null
            }
            val reject = { reason: String ->
                log.warn { "Rejecting redirect url '$url': $reason." }
                null
            }
            // Backslashes: some browsers normalize \\host and /\host to //host, i. e. to a foreign host.
            val normalized = url.replace('\\', '/')
            return when {
                SCHEME_REGEX.containsMatchIn(normalized) -> reject("absolute url (scheme given)")
                normalized.startsWith("//") -> reject("protocol relative url (foreign host)")
                !normalized.startsWith("/") -> reject("not an absolute path of this application")
                else -> url
            }
        }

        /**
         * `http:`, `javascript:`, `data:`, … - anything that isn't a path of this application.
         */
        private val SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
    }
}
