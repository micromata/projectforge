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

package org.projectforge.gateway.push

import jakarta.servlet.AsyncContext
import jakarta.servlet.DispatcherType
import jakarta.servlet.RequestDispatcher
import jakarta.servlet.ServletConnection
import jakarta.servlet.ServletContext
import jakarta.servlet.ServletInputStream
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import jakarta.servlet.http.HttpUpgradeHandler
import jakarta.servlet.http.Part
import java.io.BufferedReader
import java.security.Principal
import java.util.*

/**
 * Minimal HttpServletRequest implementation for internal ICS generation calls.
 * Only implements getParameter() and getQueryString() which are needed by CalendarSubscriptionServiceRest.
 */
class MockIcsRequest(private val userId: Long, private val q: String) : HttpServletRequest {

    override fun getParameter(name: String): String? = when (name) {
        "user" -> userId.toString()
        "q" -> q
        else -> null
    }

    override fun getQueryString(): String = "user=$userId&q=$q"

    override fun getRemoteAddr(): String = "127.0.0.1"

    // Required but unused methods — minimal stubs
    override fun getAttribute(name: String?): Any? = null
    override fun getAttributeNames(): Enumeration<String> = Collections.emptyEnumeration()
    override fun getCharacterEncoding(): String? = null
    override fun setCharacterEncoding(env: String?) {}
    override fun getContentLength(): Int = -1
    override fun getContentLengthLong(): Long = -1
    override fun getContentType(): String? = null
    override fun getInputStream(): ServletInputStream = throw UnsupportedOperationException()
    override fun getParameterNames(): Enumeration<String> = Collections.enumeration(listOf("user", "q"))
    override fun getParameterValues(name: String?): Array<String>? = null
    override fun getParameterMap(): Map<String, Array<String>> = mapOf(
        "user" to arrayOf(userId.toString()),
        "q" to arrayOf(q)
    )
    override fun getProtocol(): String = "HTTP/1.1"
    override fun getScheme(): String = "https"
    override fun getServerName(): String = "localhost"
    override fun getServerPort(): Int = 8443
    override fun getReader(): BufferedReader = throw UnsupportedOperationException()
    override fun getRemoteHost(): String = "localhost"
    override fun setAttribute(name: String?, o: Any?) {}
    override fun removeAttribute(name: String?) {}
    override fun getLocale(): Locale = Locale.getDefault()
    override fun getLocales(): Enumeration<Locale> = Collections.enumeration(listOf(Locale.getDefault()))
    override fun isSecure(): Boolean = true
    override fun getRequestDispatcher(path: String?): RequestDispatcher? = null
    override fun getRemotePort(): Int = 0
    override fun getLocalName(): String = "localhost"
    override fun getLocalAddr(): String = "127.0.0.1"
    override fun getLocalPort(): Int = 8443
    override fun getServletContext(): ServletContext = throw UnsupportedOperationException()
    override fun startAsync(): AsyncContext = throw UnsupportedOperationException()
    override fun startAsync(servletRequest: ServletRequest?, servletResponse: ServletResponse?): AsyncContext = throw UnsupportedOperationException()
    override fun isAsyncStarted(): Boolean = false
    override fun isAsyncSupported(): Boolean = false
    override fun getAsyncContext(): AsyncContext = throw UnsupportedOperationException()
    override fun getDispatcherType(): DispatcherType = DispatcherType.REQUEST
    override fun getRequestId(): String = UUID.randomUUID().toString()
    override fun getProtocolRequestId(): String = ""
    override fun getServletConnection(): ServletConnection = throw UnsupportedOperationException()
    override fun getAuthType(): String? = null
    override fun getCookies(): Array<Cookie>? = null
    override fun getDateHeader(name: String?): Long = -1
    override fun getHeader(name: String?): String? = null
    override fun getHeaders(name: String?): Enumeration<String> = Collections.emptyEnumeration()
    override fun getHeaderNames(): Enumeration<String> = Collections.emptyEnumeration()
    override fun getIntHeader(name: String?): Int = -1
    override fun getMethod(): String = "GET"
    override fun getPathInfo(): String? = null
    override fun getPathTranslated(): String? = null
    override fun getContextPath(): String = ""
    override fun getRemoteUser(): String? = null
    override fun isUserInRole(role: String?): Boolean = false
    override fun getUserPrincipal(): Principal? = null
    override fun getRequestedSessionId(): String? = null
    override fun getRequestURI(): String = "/export/ProjectForge.ics"
    override fun getRequestURL(): StringBuffer = StringBuffer("https://localhost/export/ProjectForge.ics")
    override fun getServletPath(): String = "/export/ProjectForge.ics"
    override fun getSession(create: Boolean): HttpSession? = null
    override fun getSession(): HttpSession? = null
    override fun changeSessionId(): String = throw UnsupportedOperationException()
    override fun isRequestedSessionIdValid(): Boolean = false
    override fun isRequestedSessionIdFromCookie(): Boolean = false
    override fun isRequestedSessionIdFromURL(): Boolean = false
    override fun authenticate(response: HttpServletResponse?): Boolean = false
    override fun login(username: String?, password: String?) {}
    override fun logout() {}
    override fun getParts(): Collection<Part> = emptyList()
    override fun getPart(name: String?): Part? = null
    override fun <T : HttpUpgradeHandler?> upgrade(handlerClass: Class<T>?): T = throw UnsupportedOperationException()
}
