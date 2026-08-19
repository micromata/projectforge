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
import org.projectforge.rest.dto.ServerData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class LoginServiceRest {
    @Autowired
    private lateinit var loginService: LoginService

    /**
     * Logs the user out and sends him to the login page of projectforge-next - the only login page of the
     * application, used by all three frontends.
     */
    fun logout(request: HttpServletRequest, response: HttpServletResponse): ResponseAction {
        loginService.logout(request, response)
        return ResponseAction(
            Constants.NEXT_LOGIN_URL,
            targetType = TargetType.CHECK_AUTHENTICATION,
        )
    }

    companion object {
        /**
         * Where a login without a requested target sends the user.
         *
         * Only the default: the requested target is the `returnUrl` of the login page, and the client keeps it
         * (see `app/login/page.tsx`). It cannot be answered here, because a successful login rotates the http
         * session (session fixation, [LoginService.internalLogin]) - anything this server put there before the
         * login is gone by now. The legacy login form carried it through that rotation in
         * [ServerData.returnToCaller]; a JSON client has no such round trip and doesn't need one.
         *
         * @param request Kept although unused: the caller has it, and where the user lands is exactly the kind of
         * decision that grows a request-dependent case again.
         */
        @Suppress("UNUSED_PARAMETER")
        fun getRedirectUrl(request: HttpServletRequest): String {
            return DEFAULT_REDIRECT_URL
        }

        /**
         * Where the user lands if there is nothing to return to: the start page of the legacy React app. Not the
         * start page of projectforge-next, because that one doesn't cover the whole application yet.
         */
        private val DEFAULT_REDIRECT_URL = "/${Constants.REACT_APP_PATH}calendar"

        /**
         * A url the user is sent to after an authentication step, so an attacker-supplied one would be an open
         * redirect (and a convincing phishing hop: the victim really did log in to ProjectForge). Only relative
         * paths within this application are accepted - the same rule the client applies in
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
            // Browsers strip tabs and newlines from inside a url before parsing it, so `ja<TAB>vascript:`
            // navigates as `javascript:` while a plain scheme check sees neither. Drop every C0 control
            // (NUL included) first, and hand the stripped form back so nothing later sees them either.
            val cleaned = url.filter { it.code > 0x1f && it.code != 0x7f }.trim()
            // Backslashes: some browsers normalize \\host and /\host to //host, i. e. to a foreign host.
            val normalized = cleaned.replace('\\', '/')
            return when {
                cleaned.isEmpty() -> reject("empty after removing control characters")
                SCHEME_REGEX.containsMatchIn(normalized) -> reject("absolute url (scheme given)")
                normalized.startsWith("//") -> reject("protocol relative url (foreign host)")
                !normalized.startsWith("/") -> reject("not an absolute path of this application")
                // `/next/../..//evil.com` is `//evil.com` once the browser normalizes it: every segment
                // looked like a path, the result is a foreign host. The query is data, so only the path
                // is examined.
                TRAVERSAL_REGEX.containsMatchIn(normalized.substringBefore('?').substringBefore('#')) ->
                    reject("path traversal (leaves this application)")

                else -> cleaned
            }
        }

        /**
         * `http:`, `javascript:`, `data:`, … - anything that isn't a path of this application.
         */
        private val SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")

        /**
         * A `..` path segment, which a browser resolves away before it navigates - so a url made only of
         * path segments can still end up naming a foreign host.
         */
        private val TRAVERSAL_REGEX = Regex("(^|/)\\.\\.(/|$)")
    }
}
