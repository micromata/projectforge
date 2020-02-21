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
import org.projectforge.SystemStatus
import org.projectforge.business.login.LoginProtection
import org.projectforge.business.multitenancy.TenantRegistry
import org.projectforge.business.multitenancy.TenantRegistryMap
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserTokenType
import org.projectforge.business.user.filter.CookieService
import org.projectforge.business.user.filter.UserFilter
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.Authentication
import org.projectforge.rest.AuthenticationOld
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

private const val USER_PREF_AREA_ACCESS_LOG_ENTRIES = "RestAuthentication.accessLog"

/**
 * Does the authentication stuff for restful requests.
 *
 * @author Daniel Ludwig (d.ludwig@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class RestAuthenticationUtils {
    @Autowired
    private lateinit var userService: UserService
    @Autowired
    private lateinit var userAuthenticationsService: UserAuthenticationsService
    @Autowired
    private lateinit var userPrefService: UserPrefService
    @Autowired
    private lateinit var cookieService: CookieService
    @Autowired
    private lateinit var systemStatus: SystemStatus

    /**
     * Tries to authenticate the user by the given authInfo object and will write the authenticated user to it. The user of
     * authInfo will be left untouched, if authentication fails.
     * @param authInfo [RestAuthenticationInfo] contains request, response, clientIPAddress.
     * @param userAttributes List of request parameters to look for the user's identifier (user name or user id).
     * @param secretAttributes List of request parameters to look for the user's secret (password or token).
     * @param required If required, an error message is logged if no authentication is present. Otherwise
     * only wrong credentials will result in error messages.
     */
    fun authenticationByRequestParameter(authInfo: RestAuthenticationInfo,
                                         userAttributes: Array<String>,
                                         secretAttributes: Array<String>,
                                         required: Boolean,
                                         authenticate: (user: String, authenticationToken: String) -> PFUserDO?) {
        val userString = getUserString(authInfo, userAttributes, required) ?: return
        val authenticationToken = getUserSecret(authInfo, secretAttributes) ?: return
        authInfo.user = authenticate(userString, authenticationToken)
        if (!authInfo.success) {
            log.error("Authentication failed for user $userString. Rest call forbidden.")
        }
    }

    /**
     * Checks also login protection (time out against brute force attack).
     */
    fun getUserString(authInfo: RestAuthenticationInfo, userAttributes: Array<String>, required: Boolean): String? {
        val userString = getAttribute(authInfo.request, *userAttributes)
        if (userString.isNullOrBlank()) {
            if (required) {
                log.error("Authentication failed, no user given by request params ${userAttributes.joinToString(", or ", "'", "'") { it }}. Rest call forbidden.")
            }
            return null
        }
        if (checkLoginProtection(authInfo.response, userString, LoginProtection.instance(), authInfo.clientIpAddress)) {
            // Access denied (time offset due to failed logins). Logging is done by check method.
            return null
        }
        return userString
    }

    fun getUserSecret(authInfo: RestAuthenticationInfo, secretAttributes: Array<String>): String? {
        val secret = getAttribute(authInfo.request, *secretAttributes)
        if (secret.isNullOrBlank()) {
            // Log message, because userString was found, but authentication token not:
            log.error("Authentication failed, no user secret (password or token) given by request params ${secretAttributes.joinToString(", or ", "'", "'") { it }}. Rest call forbidden.")
            return null
        }
        return secret
    }

    fun basicAuthentication(authInfo: RestAuthenticationInfo,
                            /**
                             * If required, an error message is logged if no authentication is present. Otherwise
                             * only wrong credentials will result in error messages.
                             */
                            required: Boolean,
                            authenticate: (user: String, password: String) -> PFUserDO?) {
        val authHeader = authInfo.request.getHeader("Authorization") ?: return
        // Try basic authorization
        val basic = StringUtils.split(authHeader)
        if (basic.size != 2 || !StringUtils.equalsIgnoreCase(basic[0], "Basic")) {
            if (required) {
                log.error("Basic authentication failed, header 'Authorization' not found.")
            }
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
        if (checkLoginProtection(authInfo.response, username, LoginProtection.instance(), clientIpAddress)) {
            // Access denied (time offset due to failed logins). Logging is done by check method.
            return
        }
        val password = credentials.substring(p + 1).trim { it <= ' ' }
        authInfo.user = authenticate(username, password)
        if (!authInfo.success) {
            log.error("Basic authentication failed for user '$username'.")
        }
    }

    /**
     * Re-usable doFilter method. Checks the system status and calls given authenticate method. In case of success, the
     * authenticated user will be put to the [ThreadLocalUserContext], calls the doFilter method and will be removed after request was finished.
     * @param request
     * @param response
     * @param authenticate The authenticate method tries to authenticate the user.
     * @param doFilter If authenticated, this method is called to proceed the request.
     */
    fun doFilter(request: ServletRequest,
                 response: ServletResponse,
                 authenticate: (authInfo: RestAuthenticationInfo) -> Unit,
                 doFilter: () -> Unit) {
        response as HttpServletResponse
        request as HttpServletRequest
        if (!systemStatus.upAndRunning) {
            log.error("System isn't up and running, all rest calls are denied. The system is may-be in start-up phase or in maintenance mode.")
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
            return
        }
        if (UserFilter.isUpdateRequiredFirst()) {
            log.warn("Update of the system is required first. Login via Rest not available. Administrators login required.")
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
            return
        }
        val authInfo = RestAuthenticationInfo(request, response)
        authenticate(authInfo)
        if (!authInfo.success) {
            LoginProtection.instance().incrementFailedLoginTimeOffset(authInfo.userString, authInfo.clientIpAddress)
            response.sendError(authInfo.resultCode?.value() ?: HttpServletResponse.SC_UNAUTHORIZED)
            return
        }
        try {
            registerUser(request, authInfo)
            doFilter()
        } finally {
            unregister(request, response, authInfo)
        }
    }

    fun registerLogAccess(request: HttpServletRequest, tokenType: UserTokenType) {
        registerLogAccess(request, tokenType.name)
    }

    fun registerLogAccess(request: HttpServletRequest, logAccessName: String) {
        val accessEntries = getUserAccessLogEntries(logAccessName)
        accessEntries.update(UserAccessLogEntry(request.getHeader("User-Agent"), request.remoteAddr))
    }


    open fun getUserAccessLogEntries(logAccessName: String): UserAccessLogEntries {
        return userPrefService.ensureEntry(USER_PREF_AREA_ACCESS_LOG_ENTRIES, logAccessName, UserAccessLogEntries())
    }

    open fun getUserAccessLogEntries(tokenType: UserTokenType): UserAccessLogEntries {
        return getUserAccessLogEntries(tokenType.name)
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
        MDC.put("session", request.session?.id)
        MDC.put("user", user.username)
        MDC.put("userAgent", request.getHeader("User-Agent"))
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
        MDC.remove("session")
        MDC.remove("userAgent")
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
         * "Authentication-User-Id" and "authenticationUserId".
         */
        val REQUEST_PARAMS_USER_ID = arrayOf(Authentication.AUTHENTICATION_USER_ID, AuthenticationOld.AUTHENTICATION_USER_ID)
        /**
         * "Authentication-Token" and "authenticationToken".
         */
        val REQUEST_PARAMS_TOKEN = arrayOf(Authentication.AUTHENTICATION_TOKEN, AuthenticationOld.AUTHENTICATION_TOKEN)
        /**
         * "Authentication-Username" and "authenticationUsername".
         */
        val REQUEST_PARAMS_USERNAME = arrayOf(Authentication.AUTHENTICATION_USERNAME, AuthenticationOld.AUTHENTICATION_USERNAME)
        /**
         * "Authentication-Password" and "authenticationPassword".
         */
        val REQUEST_PARAMS_PASSWORD = arrayOf(Authentication.AUTHENTICATION_PASSWORD, AuthenticationOld.AUTHENTICATION_PASSWORD)

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
