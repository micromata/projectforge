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

package org.projectforge.business.user

import org.apache.commons.lang3.StringUtils
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.Crypt
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


/**
 * The authentication tokens are used to prevent the usage of the user's password for services as calendar subscription of CardDAV/CalDAVServices as well
 * as for rest clients.
 * The tokens will be stored encrypted in the database by a key stored in ProjectForge's config file. Therefore a data base administrator isn't able to re-use
 * tokens without the knowledge of this key.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class UserAuthenticationsService {
    @Autowired
    private lateinit var userAuthenticationsDao: UserAuthenticationsDao

    /**
     * @see UserAuthenticationsDao.getUserByToken
     */
    open fun getUserByToken(userId: Int, type: UserTokenType, token: String?): PFUserDO? {
        return userAuthenticationsDao.getUserByToken(userId, type, token)
    }

    /**
     * @see UserAuthenticationsDao.getUserByToken
     */
    open fun getUserByToken(username: String, type: UserTokenType, token: String?): PFUserDO? {
        return userAuthenticationsDao.getUserByToken(username, type, token)
    }

    /**
     * @see UserAuthenticationsDao.getToken
     */
    open fun getToken(userId: Int, type: UserTokenType): String? {
        return userAuthenticationsDao.getToken(userId, type)
    }

    /**
     * @see UserAuthenticationsDao.getToken
     */
    open fun getToken(username: String, type: UserTokenType): String? {
        return userAuthenticationsDao.getToken(username, type)
    }

    /**
     * @see UserAuthenticationsDao.renewToken
     */
    open fun renewToken(userId: Int, type: UserTokenType) {
        userAuthenticationsDao.renewToken(userId, type)
    }

    /**
     * Decrypts a given string encrypted with selected token (selected by UserTokenType).
     * This is used e. g. by CalendarSubscription methods for encrypting the url containing the authentication token.
     *
     * @param userId
     * @param encryptedString
     * @return The decrypted string.
     * @see Crypt.decrypt
     */
    open fun decrypt(userId: Int, type: UserTokenType, encryptedString: String): String? {
        val storedAuthenticationToken: String? = getToken(userId, type)
        if (storedAuthenticationToken == null) {
            log.warn("Can't get authentication token for user $userId. So can't decrypt encrypted string.")
            return ""
        }
        val authenticationToken: String = StringUtils.rightPad(storedAuthenticationToken, 32, "x")
        return Crypt.decrypt(authenticationToken, encryptedString)
    }

    /**
     * Encrypts the given str with AES. The key is the selected authenticationToken of the given user (by id) (first 16
     * bytes of it).
     *
     * @param userId
     * @param data
     * @return The base64 encoded result (url safe).
     * @see Crypt.encrypt
     */
    open fun encrypt(userId: Int, type: UserTokenType, data: String): String? {
        val storedAuthenticationToken: String? = getToken(userId, type)
        if (storedAuthenticationToken == null) {
            log.warn("Can't get authentication token for user $userId. So can't encrypt string.")
            return ""
        }
        val authenticationToken: String = StringUtils.rightPad(storedAuthenticationToken, 32, "x")
        return Crypt.encrypt(authenticationToken, data)
    }

    /**
     * Uses the context user.
     *
     * @param type The token to use for encryption.
     * @param data
     * @return
     */
    open fun encrypt(type: UserTokenType, data: String): String? {
        return encrypt(ThreadLocalUserContext.getUserId(), type, data)
    }

    private val log = LoggerFactory.getLogger(UserAuthenticationsDao::class.java)

}
