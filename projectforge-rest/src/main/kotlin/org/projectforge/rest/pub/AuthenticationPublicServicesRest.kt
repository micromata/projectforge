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

package org.projectforge.rest.pub

import mu.KotlinLogging
import org.projectforge.business.configuration.DomainService
import org.projectforge.business.user.UserAuthenticationsDao
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.model.rest.UserObject
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping(Rest.PUBLIC_URL)
open class AuthenticationPublicServicesRest {
    class Credentials(val username: String, val uid: Int, val authenticationToken: String, val url: String)

    internal class TemporaryToken(val uid: Int, val systemTimeInMillis: Long, val token: String)

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
    @GET
    @Path("getAuthenticationCredentials")
    @Produces(MediaType.APPLICATION_JSON)
    open fun getAuthenticationCredentials(@QueryParam("q") q: String): Credentials {
        val temporaryToken = checkQuery(q)
        val uid = temporaryToken.uid
        val authenticationToken = userAuthenticationsDao.internalGetToken(uid, UserTokenType.REST_CLIENT)
        if (authenticationToken == null) {
            log.error { "Oups, no authentication token found for user with id $uid." }
            throw IllegalArgumentException("Invalid call.")
        }
        val user = userDao.internalGetById(uid)
        if (user == null) {
            log.error { "Oups, no user with id $uid found." }
            throw IllegalArgumentException("Invalid call.")
        }
        return Credentials(user.username ?: "unknown", uid, authenticationToken, domainService.domain)
    }

    /**
     * Creates the parameter q for the service "getAuthenticationCredentials".
     */
    fun createQueryParam(uid: Int): String {
        return createQueryParam(uid, System.currentTimeMillis())
    }

    /**
     * Internal usage for test cases.
     */
    internal fun createQueryParam(uid: Int, currentTimeInMillis: Long): String {
        cleanTemporaryToken()
        val token = NumberHelper.getSecureRandomAlphanumeric(20)
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
        cleanTemporaryToken()
        val temporaryToken = temporaryTokenList.firstOrNull { it.token == q } ?: run {
            log.error { "Temporary token '$q' not found (expired or has been never exist)." }
            throw IllegalArgumentException("Invalid call.")
        }
        val delta = System.currentTimeMillis() - temporaryToken.systemTimeInMillis
        if (delta !in 0..EXPIRE_TIME_IN_MILLIS) {
            log.error { "Request token q=$q expired: ${PFDateTime.from(temporaryToken.systemTimeInMillis).isoStringMilli}" }
            throw IllegalArgumentException("Request is expired. Try to get a new token.")
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
    }
}
