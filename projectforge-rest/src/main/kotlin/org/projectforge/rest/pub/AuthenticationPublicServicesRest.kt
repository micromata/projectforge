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
import mu.KotlinLogging
import org.projectforge.business.configuration.DomainService
import org.projectforge.business.login.LoginProtection
import org.projectforge.business.user.UserAuthenticationsDao
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.model.rest.UserObject
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.RestResolver
import org.projectforge.security.ConstantTimeCompare
import org.projectforge.security.SecurityLogging
import org.projectforge.web.WebUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * This method supports mobile clients for faster initial authentication. A 2d barcode will be present
 * by ProjectForge (MyAccount -> Rest token -> info) for scanning user credentials.
 */
@RestController
@RequestMapping(Rest.PUBLIC_URL)
open class AuthenticationPublicServicesRest {
    class Credentials(val username: String, val uid: Long, val authenticationToken: String, val url: String)

    internal class TemporaryToken(val uid: Long, val systemTimeInMillis: Long, val token: String)

    @Autowired
    private lateinit var domainService: DomainService

    @Autowired
    private lateinit var userAuthenticationsDao: UserAuthenticationsDao

    @Autowired
    private lateinit var userDao: UserDao

    /**
     * Stores the currently generated temporary tokens (valid for 2 minutes). Will be cleaned up.
     */
    private val temporaryTokenList = mutableListOf<TemporaryToken>()

    /**
     * Get url, authentication token and user name.
     *
     * This method supports mobile clients for faster initial authentication.
     *
     * @param q This param is provided by MyAccount page as bar code and contains a temporarily valid token (2 minutes).
     *
     * @return [UserObject]
     */
    @GetMapping(AUTHENTICATION_CREDENTIALS_PATH)
    open fun getAuthenticationCredentials(
        request: HttpServletRequest,
        @RequestParam("q") q: String
    ): ResponseEntity<Any> {
        // This endpoint hands out a REST_CLIENT token to anybody knowing q, and it is public (no user filter for
        // /rsPublic). So guessing q must be throttled, and a failure must not be more expensive for us than for the
        // caller: no exception (that would be a 500 with a stack trace plus a mail to the developers, see
        // GlobalExceptionRegistry.sendMailToDevelopers), just a 400.
        val clientIp = WebUtils.getClientIp(request)
        val loginProtection = LoginProtection.instance()
        // The ip address is passed as the user key (and not as clientIpAddress) on purpose: only the user key
        // honours AUTHENTICATION_TYPE as a namespace, and the ip map has a threshold of 1000 failed attempts.
        val offset = loginProtection.getFailedLoginTimeOffsetIfExists(clientIp, null, AUTHENTICATION_TYPE)
        if (offset > 0) {
            SecurityLogging.logSecurityWarn(
                request, this::class.java, "REST AUTHENTICATION DENIED",
                "Access denied for ${offset / 1000} seconds due to failed attempts with an invalid token q."
            )
            return ResponseEntity.badRequest().body("Invalid call.")
        }
        val temporaryToken = findTemporaryToken(q) ?: run {
            loginProtection.incrementFailedLoginTimeOffset(clientIp, null, AUTHENTICATION_TYPE)
            SecurityLogging.logSecurityWarn(
                request, this::class.java, "REST AUTHENTICATION FAILED",
                "Temporary token not found (expired or has never been existing)."
            )
            return ResponseEntity.badRequest().body("Invalid call.")
        }
        loginProtection.clearLoginTimeOffset(clientIp, null, null, AUTHENTICATION_TYPE)
        val uid = temporaryToken.uid
        val authenticationToken = userAuthenticationsDao.internalGetToken(uid, UserTokenType.REST_CLIENT)
        if (authenticationToken == null) {
            val msg = "Oups, no authentication token found for user with id $uid."
            log.error(msg)
            SecurityLogging.logSecurityWarn(this::class.java, "REST AUTHENTICATION FAILED", msg)
            return ResponseEntity.badRequest().body("Invalid call.")
        }
        val user = userDao.find(uid, checkAccess = false)
        if (user == null) {
            val msg = "Oups, no user with id $uid found."
            log.error(msg)
            SecurityLogging.logSecurityWarn(this::class.java, "REST AUTHENTICATION FAILED", msg)
            return ResponseEntity.badRequest().body("Invalid call.")
        }
        return ResponseEntity.ok(Credentials(user.username ?: "unknown", uid, authenticationToken, domainService.domain))
    }

    /**
     * Creates the parameter q for the service "getAuthenticationCredentials".
     */
    open fun createQueryURL(): String {
        val token = createTemporaryToken()
        return domainService.getDomain(RestResolver.getPublicRestUrl(this::class.java, "$AUTHENTICATION_CREDENTIALS_PATH?q=$token", true))
    }

    /**
     * Internal usage for test cases.
     */
    internal open fun createTemporaryToken(): String {
        val uid = ThreadLocalUserContext.loggedInUserId!!
        return createTemporaryToken(uid, System.currentTimeMillis())
    }

    /**
     * Internal usage for test cases.
     */
    internal open fun createTemporaryToken(uid: Long, currentTimeInMillis: Long): String {
        cleanTemporaryToken()
        val token = NumberHelper.getSecureRandomAlphanumeric(TEMPORARY_TOKEN_LENGTH)
        synchronized(temporaryTokenList) {
            temporaryTokenList.add(TemporaryToken(uid, currentTimeInMillis, token))
        }
        return token
    }

    /**
     * Tries to get the temporary token. If doesn't exist or was expired, an exception is thrown.
     *
     * @return The valid and not expired token.
     */
    internal fun checkQuery(q: String): TemporaryToken {
        return findTemporaryToken(q) ?: throw IllegalArgumentException("Invalid call.")
    }

    /**
     * Tries to get the temporary token.
     *
     * @return The valid and not expired token or null, if no such token exists. The token isn't part of the log
     * message: it is a credential (whoever knows it gets a REST_CLIENT token), so it doesn't belong into a log file.
     */
    private fun findTemporaryToken(q: String): TemporaryToken? {
        cleanTemporaryToken()
        val temporaryToken = synchronized(temporaryTokenList) {
            temporaryTokenList.firstOrNull { ConstantTimeCompare.equals(it.token, q) }
        } ?: return null
        val delta = System.currentTimeMillis() - temporaryToken.systemTimeInMillis
        if (delta !in 0..EXPIRE_TIME_IN_MILLIS) {
            log.warn { "Request token expired: ${PFDateTime.from(temporaryToken.systemTimeInMillis).isoStringMilli}" }
            return null
        }
        return temporaryToken
    }

    private fun cleanTemporaryToken() {
        synchronized(temporaryTokenList) {
            val currentTimeInMillis = System.currentTimeMillis()
            temporaryTokenList.removeIf { currentTimeInMillis - it.systemTimeInMillis > EXPIRE_TIME_IN_MILLIS }
        }
    }

    companion object {
        /**
         * Expire time is 2 minutes.
         */
        internal const val EXPIRE_TIME_IN_MILLIS = 120 * 1000L

        private const val TEMPORARY_TOKEN_LENGTH = 20

        private const val AUTHENTICATION_CREDENTIALS_PATH = "authenticationCredentials"

        /**
         * Own namespace of [LoginProtection], so that failed attempts here neither lock out a user's login nor are
         * lifted by one.
         */
        private const val AUTHENTICATION_TYPE = "REST_CREDENTIALS"
    }
}
