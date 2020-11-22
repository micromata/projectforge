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
import org.projectforge.framework.utils.Crypt
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

    @Autowired
    private lateinit var domainService: DomainService

    @Autowired
    private lateinit var userAuthenticationsDao: UserAuthenticationsDao

    @Autowired
    private lateinit var userDao: UserDao

    /**
     * Get url, authentication token and user name. The given query param contains the server time of bar code creation
     * crypted by the user's authentication token and user name. The authentication token is only known by user.
     *
     * This method supports mobile clients for faster initial authentication.
     *
     * @param q This param is provided by MyAccount page as bar code and contains a temporarily valid token (1 minute).
     * @param uid The user's id to check and get the credentials.
     *
     * @return [UserObject]
     */
    @GET
    @Path("getAuthenticationCredentials")
    @Produces(MediaType.APPLICATION_JSON)
    open fun getAuthenticationCredentials(@QueryParam("q") q: String, @QueryParam("uid") uid: Int): Credentials {
        val user = userDao.internalGetOrLoad(uid)
        if (user == null) {
            log.error { "User with uid=$uid not found (attempt to fraud?)." }
            throw IllegalArgumentException("Invalid call.")
        }
        val token = checkQuery(q, uid)
        return Credentials(user.username ?: "unknown", uid, token, domainService.domain)
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
        val token = userAuthenticationsDao.getToken(uid, UserTokenType.REST_CLIENT)
        return Crypt.encrypt(token, "$currentTimeInMillis")
    }

    /**
     * Tries to decrypt given q by authentication token of given user (by uid) and checks, if the decrypted
     * time in millis is not expired.
     */
    internal fun checkQuery(q: String, uid: Int): String {
        val token = userAuthenticationsDao.internalGetToken(uid, UserTokenType.REST_CLIENT)
        if (token == null) {
            log.error { "Authentication token for user with uid=$uid not found. (attempt to fraud?)" }
            throw IllegalArgumentException("Invalid call.")
        }
        val decrypted = Crypt.decrypt(token, q)
        if (decrypted == null) {
            log.error { "Can't decrypt q=$q for user uid=$uid (attempt to fraud?)" }
            throw IllegalArgumentException("Invalid call.")
        }
        val timeInMillis = NumberHelper.parseLong(decrypted) ?: run {
            log.error { "Decrypted q=$q for user uid=$uid isn't a system time (attempt to fraud?): $decrypted" }
            throw IllegalArgumentException("Invalid call.")
        }
        val delta = System.currentTimeMillis() - timeInMillis
        if (delta < 0) {
            log.error { "Oups: decrypted time is in the future (attempt to fraud?): ${PFDateTime.from(timeInMillis).isoStringMilli}." }
            throw IllegalArgumentException("Invalid call.")
        }
        if (delta > EXPIRE_TIME_IN_MILLIS) {
            log.error { "Request token q=$q expired for user uid=$uid: ${PFDateTime.from(timeInMillis).isoStringMilli}" }
            throw IllegalArgumentException("Request is expired. Try to get a new token.")
        }
        return token
    }

    companion object {
        /**
         * Expire time is 1 minute.
         */
        const val EXPIRE_TIME_IN_MILLIS = 60 * 1000
    }
}
