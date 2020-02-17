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

package org.projectforge.web.rest

import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.login.LoginProtection
import org.projectforge.business.multitenancy.TenantRegistry
import org.projectforge.business.multitenancy.TenantRegistryMap
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.filter.CookieService
import org.projectforge.business.user.filter.UserFilter
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.Authentication
import org.projectforge.rest.ConnectionSettings
import org.projectforge.rest.converter.DateTimeFormat
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Does the authentication stuff for restfull requests.
 *
 * @author Daniel Ludwig (d.ludwig@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
class RestAuthenticationUtils {
    @Autowired
    private lateinit var userService: UserService
    @Autowired
    private lateinit var userAuthenticationsService: UserAuthenticationsService
    @Autowired
    private lateinit var cookieService: CookieService

    fun authenticationByRequestParameter(authInfo: RestAuthenticationInfo,
                                         requestParamsDef: RequestParamsDef,
                                         authenticate: (user: String, authenticationToken: String) -> PFUserDO?) {
        val userString = getAttribute(authInfo.request, *requestParamsDef.userAttributes)
        if (userString == null) {
            log.error("Authentication failed, no user given by '${Authentication.AUTHENTICATION_USER_ID}'. Rest call forbidden.")
            return
        }
        if (checkLoginProtection(authInfo.response, userString, LoginProtection.instance(), authInfo.clientIpAddress)) { // access denied
            return
        }
        val authenticationToken = getAttribute(authInfo.request, *requestParamsDef.tokenAttributes)
        if (authenticationToken.isNullOrBlank()) {
            log.error("${Authentication.AUTHENTICATION_TOKEN} not found. Rest call forbidden.")
            return
        }
        authInfo.user = authenticate(userString, authenticationToken)
        if (!authInfo.success) {
            log.error("Authentication failed for user $userString. Rest call forbidden.")
        }
    }

    fun basicAuthentication(authInfo: RestAuthenticationInfo, authenticate: (user: String, password: String) -> PFUserDO?) {
        val authHeader = authInfo.request.getHeader("Authorization") ?: return
        // Try basic authorization
        val basic = StringUtils.split(authHeader)
        if (basic.size != 2 || !StringUtils.equalsIgnoreCase(basic[0], "Basic")) {
            return
        }
        val credentials = String(Base64.decodeBase64(basic[1]), StandardCharsets.UTF_8)
        val p = credentials.indexOf(":")
        if (p < 1) {
            log.error("Basic authentication failed, credentials not of format 'user:password'.")
            return
        }
        val username = credentials.substring(0, p).trim { it <= ' ' }
        val clientIpAddress = authInfo.request.remoteAddr
        if (checkLoginProtection(authInfo.response, username, LoginProtection.instance(), clientIpAddress)) { // access denied
            return
        }
        val password = credentials.substring(p + 1).trim { it <= ' ' }
        authInfo.user = authenticate(username, password)
        if (!authInfo.success) {
            log.error("Basic authentication failed for user '$username'.")
        }
    }

    /**
     * You must use try { registerUser(...) } finally { unregisterUser() }!!!!
     *
     * @param request
     * @param userInfo
     */
    fun registerUser(request: ServletRequest, userInfo: RestAuthenticationInfo) {
        val user = userInfo.user!!
        val clientIpAddress = userInfo.clientIpAddress
        LoginProtection.instance().clearLoginTimeOffset(userInfo.userString, clientIpAddress)
        ThreadLocalUserContext.setUser(userGroupCache, user)
        val req = request as HttpServletRequest
        val settings = getConnectionSettings(req)
        ConnectionSettings.set(settings)
        val ip = request.getRemoteAddr()
        if (ip != null) {
            MDC.put("ip", ip)
        } else { // Only null in test case:
            MDC.put("ip", "unknown")
        }
        MDC.put("user", user.username)
        log.info("User: " + user.username + " calls RestURL: " + request.requestURI
                + " with ip: "
                + clientIpAddress)
    }

    fun unregister(request: ServletRequest, response: ServletResponse,
                   userInfo: RestAuthenticationInfo) {
        ThreadLocalUserContext.setUser(userGroupCache, null)
        ConnectionSettings.set(null)
        MDC.remove("ip")
        MDC.remove("user")
        val resultCode = (response as HttpServletResponse).status
        if (resultCode != HttpStatus.OK.value() && resultCode != HttpStatus.MULTI_STATUS.value()) { // MULTI_STATUS (207) will be returned by milton.io (CalDAV/CardDAV), because XML is returned.
            val user = userInfo.user!!
            val clientIpAddress = userInfo.clientIpAddress
            log.error("User: " + user.username + " calls RestURL: " + (request as HttpServletRequest).requestURI
                    + " with ip: "
                    + clientIpAddress
                    + ": Response status not OK: status=" + response.status
                    + ".")
        }
    }

    @Throws(IOException::class)
    private fun checkLoginProtection(response: HttpServletResponse, userString: String?,
                                     loginProtection: LoginProtection,
                                     clientIpAddress: String): Boolean {
        val offset = loginProtection.getFailedLoginTimeOffsetIfExists(userString, clientIpAddress)
        if (offset > 0) {
            val seconds = (offset / 1000).toString()
            log.warn("The account for '"
                    + userString
                    + "' is locked for "
                    + seconds
                    + " seconds due to failed login attempts (ip=" + clientIpAddress + ").")
            response.sendError(HttpServletResponse.SC_FORBIDDEN)
            return true
        }
        return false
    }

    private fun getConnectionSettings(req: HttpServletRequest): ConnectionSettings {
        val settings = ConnectionSettings()
        val dateTimeFormatString = getAttribute(req, ConnectionSettings.DATE_TIME_FORMAT)
        if (dateTimeFormatString != null) {
            val dateTimeFormat = DateTimeFormat.valueOf(dateTimeFormatString.toUpperCase())
            if (dateTimeFormat != null) {
                settings.dateTimeFormat = dateTimeFormat
            }
        }
        return settings
    }

    private val tenantRegistry: TenantRegistry
        get() = TenantRegistryMap.getInstance().tenantRegistry

    private val userGroupCache: UserGroupCache
        get() = tenantRegistry.userGroupCache

    companion object {
        private val log = LoggerFactory.getLogger(RestAuthenticationUtils::class.java)

        fun executeLogin(request: HttpServletRequest?, userContext: UserContext?) { // Wicket part: (page.getSession() as MySession).login(userContext, page.getRequest())
            UserFilter.login(request, userContext)
        }

        /**
         * @param req
         * @param keys Name of the parameter key. Additional keys may be given as alternative keys if first key isn't found.
         * Might be used for backwards compatibility.
         * @return
         */
        internal fun getAttribute(req: HttpServletRequest, vararg keys: String): String? {
            keys.forEach { key ->
                var value = req.getHeader(key)
                if (value == null) {
                    value = req.getParameter(key)
                }
                if (value != null) {
                    return value
                }
            }
            return null
        }
    }
}
