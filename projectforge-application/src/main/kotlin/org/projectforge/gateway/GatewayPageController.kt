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

package org.projectforge.gateway

import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.user.UserLocale
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.login.LoginService
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.webmvc.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.web.WebAttributes
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.util.HtmlUtils
import java.net.URI
import java.util.Locale

/**
 * The few pages of the gateway outside the projectforge-next app: the start page (redirects to the data transfer),
 * the login page and the error page. All of them show whether and as whom the user is logged in.
 *
 * The next client goes to /next/login if not logged in, which on the gateway is this login page instead of the app's
 * own (username and password, the gateway knows OAuth2 only): the app navigates there client side, the gateway
 * refuses the page's RSC payload, and Next falls back to loading the url as a page, which ends here.
 */
@Controller
@ConditionalOnProperty(name = ["projectforge.gateway.enabled"], havingValue = "true")
class GatewayPageController(
    private val clientRegistrationRepository: ObjectProvider<ClientRegistrationRepository>,
) : ErrorController {

    /** "/next/" is the app's start page, which the gateway doesn't serve: the logo and an access refusal lead there. */
    @GetMapping("/", "/next", "/next/")
    fun index(): ResponseEntity<String> = redirect("$DATATRANSFER_URL/")

    @GetMapping(LOGIN_URL, "$LOGIN_URL/")
    fun login(
        request: HttpServletRequest,
        @RequestParam(required = false) returnUrl: String?,
        @RequestParam(required = false) error: String?,
    ): ResponseEntity<String> {
        val page = Page(request)
        if (error == null && page.pfUser != null) {
            // Already logged in (e. g. the React client lost its state): continue.
            return redirect(returnUrl?.let { safeReturnUrl(it) } ?: "$DATATRANSFER_URL/")
        }
        val message = if (error != null) {
            val exception = request.getSession(false)?.let { session ->
                (session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) as? AuthenticationException).also {
                    session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION)
                }
            }
            page.translate("gateway.page.login.failed", exception?.message ?: error)
        } else {
            null
        }
        return page.render(HttpStatus.OK, page.translate("gateway.page.login"), message)
    }

    @RequestMapping("/error")
    fun error(request: HttpServletRequest): ResponseEntity<String> {
        val page = Page(request)
        val statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) as? Int
        val status = statusCode?.let { HttpStatus.resolve(it) } ?: HttpStatus.INTERNAL_SERVER_ERROR
        val message = when (status) {
            HttpStatus.NOT_FOUND -> page.translate("gateway.page.error.notFound")
            HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED -> page.translate("gateway.page.error.forbidden")
            else -> page.translate("gateway.page.error.other")
        }
        return page.render(status, page.translate("gateway.page.error.title", status.value()), message)
    }

    private inner class Page(request: HttpServletRequest) {
        /** The PF user of the session, see [GatewaySessionFilter]. */
        val pfUser: PFUserDO? = LoginService.getUser(request)

        /**
         * The identity of the OAuth2 login. Read from the session rather than the SecurityContextHolder: during an
         * error dispatch, the holder is already cleared.
         */
        val oidcUser = (request.getSession(false)
            ?.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) as? SecurityContext)
            ?.authentication?.principal as? OidcUser

        val locale: Locale = UserLocale.determineUserLocale(pfUser, request = request)

        fun translate(key: String, vararg params: Any?): String = I18nHelper.getLocalizedMessage(locale, key, *params)

        fun render(status: HttpStatus, title: String, message: String?): ResponseEntity<String> {
            val loginState = when {
                pfUser != null -> translate("gateway.page.loggedInAs", pfUser.getFullname().ifBlank { pfUser.username ?: "" })
                oidcUser != null -> translate("gateway.page.userUnknown", oidcUser.preferredUsername ?: oidcUser.subject)
                else -> translate("gateway.page.notLoggedIn")
            }
            val links = mutableListOf<Pair<String, String>>()
            if (pfUser != null) {
                links.add(DATATRANSFER_URL to translate("gateway.page.datatransfer"))
            } else {
                loginUrl?.let { links.add(it to translate("gateway.page.login")) }
            }
            if (pfUser != null || oidcUser != null) {
                links.add(LOGOUT_URL to translate("gateway.page.logout"))
            }
            val html = """
                |<!DOCTYPE html>
                |<html lang="${locale.language}">
                |<head>
                |<meta charset="UTF-8">
                |<meta name="viewport" content="width=device-width, initial-scale=1">
                |<title>ProjectForge - ${esc(title)}</title>
                |<style>
                |body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #f4f5f7; color: #222; margin: 0; }
                |main { max-width: 32rem; margin: 10vh auto; background: #fff; border-radius: 8px; padding: 2rem; box-shadow: 0 1px 4px rgba(0,0,0,.15); }
                |h1 { font-size: 1.4rem; margin-top: 0; }
                |.state { color: #555; }
                |a.button { display: inline-block; margin: .5rem .5rem 0 0; padding: .5rem 1rem; border-radius: 4px; background: #1d63b8; color: #fff; text-decoration: none; }
                |a.button.secondary { background: #6c757d; }
                |</style>
                |</head>
                |<body>
                |<main>
                |<h1>${esc(title)}</h1>
                |${message?.let { "<p>${esc(it)}</p>" } ?: ""}
                |<p class="state">${esc(loginState)}</p>
                |<p>${links.mapIndexed { i, (url, label) -> "<a class=\"button${if (i > 0) " secondary" else ""}\" href=\"${esc(url)}\">${esc(label)}</a>" }.joinToString(" ")}</p>
                |</main>
                |</body>
                |</html>
                |""".trimMargin()
            return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML).body(html)
        }
    }

    /**
     * The authorization URL of the (only) OAuth2 client registration, or null if OAuth2 isn't configured.
     */
    private val loginUrl: String?
        get() {
            @Suppress("UNCHECKED_CAST")
            val registrations = clientRegistrationRepository.ifAvailable as? Iterable<ClientRegistration>
            return registrations?.firstOrNull()?.let { "/oauth2/authorization/${it.registrationId}" }
        }

    private fun esc(text: String): String = HtmlUtils.htmlEscape(text)

    private fun redirect(url: String): ResponseEntity<String> =
        ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build()

    companion object {
        const val DATATRANSFER_URL = "/next/datatransfer"
        const val LOGIN_URL = "/next/login"
        const val LOGOUT_URL = "/logout"

        /**
         * The url to continue with after the login, if it is one of the data transfer pages. The next client names it
         * without the app's base path ("/datatransfer/42", see AuthGuard), which is added then.
         *
         * Only relative URLs: no open redirect to foreign hosts ("//host", "/\host").
         */
        internal fun safeReturnUrl(url: String): String? {
            if (url.contains("//") || url.contains("\\")) return null
            val absolute = if (url.startsWith(APP_DATATRANSFER_ROUTE)) "/next$url" else url
            return absolute.takeIf { it == DATATRANSFER_URL || it.startsWith("$DATATRANSFER_URL/") || it.startsWith("$DATATRANSFER_URL?") }
        }

        /** The route of the data transfer within the next app, i.e. without its base path. */
        private const val APP_DATATRANSFER_ROUTE = "/datatransfer"
    }
}
