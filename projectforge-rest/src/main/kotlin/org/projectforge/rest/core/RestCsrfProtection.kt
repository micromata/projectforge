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

package org.projectforge.rest.core

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.projectforge.framework.configuration.PFSpringConfiguration
import org.projectforge.rest.utils.RequestLog
import org.projectforge.security.SecurityLogging
import org.projectforge.web.rest.RestAuthenticationInfo
import org.projectforge.web.rest.RestAuthenticationUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Central CSRF protection of all rest calls under [org.projectforge.rest.config.Rest.URL], applied by
 * [RestAuthenticationUtils.doFilter].
 *
 * The session is identified by the `JSESSIONID` cookie, which the browser sends on every request -
 * including one triggered by a foreign site. Two independent barriers guard against that:
 *
 * 1. **[checkSameSite] (all clients, all methods).** `Sec-Fetch-Site` is set by the browser itself and
 *    cannot be forged by the calling page, so it is the only barrier that also covers the legacy
 *    state-changing `@GetMapping`s (`filter/delete`, `filter/rename`, `filterReset`, `cancel`, …),
 *    which are triggerable by a plain `<img src>` and carry no token anywhere.
 * 2. **[checkToken] (projectforge-next, state-changing methods).** The session's token from
 *    [SessionCsrfService], sent by projectforge-next in the [CSRF_TOKEN_HEADER]. Covers browsers too old
 *    to send `Sec-Fetch-Site`. A header is used rather than a body field: it also works for calls
 *    without a body and cannot be set cross-site without a preflight.
 *
 *    Only next clients are checked here, because only they send the header: the UILayout clients carry
 *    their token in `PostData.serverData` and are validated inside the endpoints
 *    ([SessionCsrfService.validateCsrfToken]) - reading the body in a filter would consume the stream.
 *    An attacker omitting the `X-PF-Frontend` header therefore skips this barrier, but not barrier 1,
 *    which is the reason barrier 1 must not be conditional on the client.
 *
 * Neither barrier applies to pure REST clients authenticated by an access token
 * ([RestAuthenticationInfo.loggedInByAuthenticationToken]): they don't rely on an ambient cookie, so
 * they cannot be tricked into a cross-site request in the first place. This mirrors the exemption in
 * [SessionCsrfService.validateCsrfToken].
 */
@Service
open class RestCsrfProtection {
    @Autowired
    private lateinit var sessionCsrfService: SessionCsrfService

    /**
     * @return true, if the request may proceed. If false is returned, the error response has already been
     * written and the caller must not call the filter chain.
     */
    fun checkRequest(request: HttpServletRequest, response: HttpServletResponse, authInfo: RestAuthenticationInfo): Boolean {
        if (authInfo.loggedInByAuthenticationToken) {
            return true // Not cookie based, see class comment.
        }
        if (request.getSession(false) == null) {
            return true // No session, so there is no ambient credential to abuse.
        }
        if (!checkSameSite(request)) {
            deny(request, response, "Cross-site request denied")
            return false
        }
        if (RestAuthenticationUtils.isNextClient(request) && isStateChangingMethod(request.method) && !checkToken(request)) {
            deny(request, response, "CSRF token check failed")
            return false
        }
        return true
    }

    /**
     * Barrier 1: rejects requests a foreign site initiated.
     *
     * `Sec-Fetch-Site` is absent for clients that are not modern browsers (curl, the DAV clients, older
     * browsers) - those fall through to the token check, which is why this must not be the only barrier.
     *
     * Residual risk: a browser too old to send the header and not identifying itself as next
     * (`X-PF-Frontend`) passes both barriers. The `SameSite=Lax` session cookie covers that for POSTs;
     * what stays open are the legacy state-changing `@GetMapping`s (`filter/delete`, `filter/rename`,
     * `filterReset`, `cancel`), which the cookie's Lax mode lets through. Fixing that means turning them
     * into POSTs, which touches the React frontend as well.
     */
    private fun checkSameSite(request: HttpServletRequest): Boolean {
        return isSameSiteRequest(request)
    }

    /**
     * Barrier 2: compares the token of the request against the one stored in the user's session.
     */
    private fun checkToken(request: HttpServletRequest): Boolean {
        return sessionCsrfService.checkToken(request, request.getHeader(CSRF_TOKEN_HEADER))
    }

    private fun deny(request: HttpServletRequest, response: HttpServletResponse, reason: String) {
        val msg = "$reason: ${request.method}:${request.requestURI}"
        log.error { "$msg (${RequestLog.asString(request)})" }
        SecurityLogging.logSecurityWarn(request, this::class.java, "CSRF CHECK FAILED", msg)
        response.status = HttpStatus.FORBIDDEN.value()
        if (RestAuthenticationUtils.isNextClient(request)) {
            // projectforge-next refreshes its token (via /rs/userStatus) and repeats the call once,
            // see lib/rs/client.ts. A ResponseAction would be unreadable for it.
            //
            // This is the normal (not the exceptional) path whenever the session was renewed under the
            // client's feet: a stay-logged-in login creates a fresh session - and therefore a fresh token -
            // inside LoginService.checkStayLoggedIn, which runs in authenticate() just before this check.
            // The retry makes that invisible to the user, whereas the UILayout clients get a
            // "errorpage.csrfError" validation message and have to submit a second time.
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write("""{"csrfTokenRequired":true}""")
        }
    }

    companion object {
        /**
         * Header used by projectforge-next (lib/rs/client.ts) to send the session's CSRF token. The UILayout
         * based clients send it inside `PostData.serverData` instead (see [SessionCsrfService]).
         */
        const val CSRF_TOKEN_HEADER = "X-PF-CSRF-Token"

        /**
         * Fetch metadata header, set by the browser and not modifiable by the calling page.
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-Fetch-Site
         */
        const val SEC_FETCH_SITE_HEADER = "Sec-Fetch-Site"

        /**
         * True, if the request wasn't initiated by a foreign page. Also used by the public endpoints, which have no
         * filter and therefore no [checkRequest] around them (see
         * [org.projectforge.rest.pub.next.LoginNextRest.getStatus]).
         *
         * Not a barrier on its own: the header is absent for everything that isn't a modern browser (curl, DAV
         * clients), and those cases return true here.
         */
        fun isSameSiteRequest(request: HttpServletRequest): Boolean {
            val site = request.getHeader(SEC_FETCH_SITE_HEADER) ?: return true
            if (site == "same-origin" || site == "none") {
                // "none" is a user-initiated navigation (bookmark, typed url), not a request of a foreign page.
                return true
            }
            // "same-site" is denied on purpose, although the session cookie would be sent (SameSite=Lax):
            // every rest call originates from a page of this very app, so it is always "same-origin". Allowing
            // "same-site" would let a compromised sibling subdomain through - and such a request wouldn't be
            // stopped by the token check either, because an attacker simply omits the X-PF-Frontend header.
            if (PFSpringConfiguration.getInstance()?.corsFilterEnabled == true) {
                // Development only: the frontend may be served by its own dev server on another origin.
                // Never enabled in production (see PFSpringConfiguration.logCorsFilterWarning).
                log.warn { "Allowing cross origin request ($SEC_FETCH_SITE_HEADER=$site), because the CORS filter is enabled: ${RequestLog.asString(request)}" }
                return true
            }
            return false
        }

        /**
         * Only these are checked for a token. The legacy state-changing GETs are covered by
         * [checkSameSite] alone (they exist for the React app and carry no token).
         */
        fun isStateChangingMethod(method: String?): Boolean {
            return when (method?.uppercase()) {
                "GET", "HEAD", "OPTIONS", "TRACE" -> false
                else -> true
            }
        }
    }
}
