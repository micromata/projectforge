/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.apache.commons.lang3.ArrayUtils
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.login.LoginHandler
import org.projectforge.business.login.PasswordCheckResult
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.user
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserPasswordDO
import org.projectforge.framework.utils.Crypt
import org.projectforge.framework.utils.Crypt.digest
import org.projectforge.framework.utils.NumberHelper.getSecureRandomAlphanumeric
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*


private val log = KotlinLogging.logger {}

/**
 * The authentication tokens are used to prevent the usage of the user's password for services as calendar subscription of CardDAV/CalDAVServices as well
 * as for rest clients.
 * The tokens will be stored encrypted in the database by a key stored in ProjectForge's config file. Therefore a data base administrator isn't able to re-use
 * tokens without the knowledge of this key.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class UserPasswordDao : BaseDao<UserPasswordDO>(UserPasswordDO::class.java) {
    @Autowired
    private lateinit var userAuthenticationsService: UserAuthenticationsService

    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var userDao: UserDao

    @PostConstruct
    private fun postConstruct() {
        pepperString = configurationService.securityConfig.passwordPepper ?: ""
    }

    /**
     * return true for admin users.
     */
    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return accessChecker.isUserMemberOfAdminGroup(user)
    }

    override fun hasAccess(
        user: PFUserDO, obj: UserPasswordDO?, oldObj: UserPasswordDO?,
        operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        require(obj?.user != null) { "UserPasswordDO must have a obj.user." }
        return hasLoggedInUserAccess(obj!!.user!!.id, throwException)
    }

    private fun hasLoggedInUserAccess(ownerUserId: Long?, throwException: Boolean = true): Boolean {
        ownerUserId ?: return false
        if (accessChecker.isLoggedInUserMemberOfAdminGroup || ownerUserId == ThreadLocalUserContext.userId) {
            return true
        }
        if (throwException) {
            throw AccessException("access.exception.violation", "AccessToken")
        }
        return false
    }

    override fun newInstance(): UserPasswordDO {
        return UserPasswordDO()
    }

    /**
     * Encrypts the password with a new generated salt string and the pepper string if configured any.
     *
     * @param user     The user to user.
     * @param clearTextPassword as clear text.
     * @see Crypt.digest
     */
    @JvmOverloads
    open fun encryptAndSavePasswordNewTrans(userId: Long, clearTextPassword: CharArray, checkAccess: Boolean = true) {
        return persistenceService.runInTransaction { context ->
            encryptAndSavePassword(userId, clearTextPassword, checkAccess, context)
        }
    }

    /**
     * Encrypts the password with a new generated salt string and the pepper string if configured any.
     *
     * @param user     The user to user.
     * @param clearTextPassword as clear text.
     * @see Crypt.digest
     */
    @JvmOverloads
    open fun encryptAndSavePassword(
        userId: Long,
        clearTextPassword: CharArray,
        checkAccess: Boolean = true,
        context: PfPersistenceContext
    ) {
        val passwords = ensurePassword(userId, checkAccess, context)
        newSaltString.let { salt ->
            passwords.passwordSalt = salt
            passwords.passwordHash = encryptAndClear(pepperString, salt, clearTextPassword)
        }
        if (passwords.id == null) {
            internalSave(passwords, context)
        } else {
            internalUpdate(passwords, context)
        }
    }


    /**
     * Doesn't save a new passwords entry (it will only be returned and it's on the caller to persist it).
     * @return Stored or created passwords object for given user.
     * @throws AccessException if the logged-in user neither doesn't match the given user nor is admin user.
     */
    private fun ensurePassword(
        userId: Long,
        checkAccess: Boolean = true,
        context: PfPersistenceContext
    ): UserPasswordDO {
        if (checkAccess) {
            hasLoggedInUserAccess(userId)
        }
        var passwordObj = internalGetByUserId(userId, context)
        if (passwordObj == null) {
            passwordObj = UserPasswordDO()
            val user = userDao.internalGetById(userId, context)
            passwordObj.user = user
        }
        return passwordObj
    }

    override fun onSaveOrModify(obj: UserPasswordDO, context: PfPersistenceContext) {
        obj.checkAndFixPassword()
    }

    open fun internalGetByUserId(userId: Long): UserPasswordDO? {
        return persistenceService.runReadOnly { context ->
            internalGetByUserId(userId, context)
        }
    }

    open fun internalGetByUserId(userId: Long, context: PfPersistenceContext): UserPasswordDO? {
        return context.selectNamedSingleResult(
            UserPasswordDO.FIND_BY_USER_ID,
            UserPasswordDO::class.java,
            Pair("userId", userId),
        )
    }

    open fun onPasswordChange(user: PFUserDO) {
        val userId = user.id ?: return
        val passwordObj = internalGetByUserId(userId) ?: return
        passwordObj.checkAndFixPassword()
        if (passwordObj.passwordHash != null) {
            user.lastPasswordChange = Date()
            if (user.id != null) {
                // Renew token only for existing users.
                userAuthenticationsService.renewToken(userId, UserTokenType.STAY_LOGGED_IN_KEY)
                userAuthenticationsService.renewToken(userId, UserTokenType.REST_CLIENT)
            }
        } else {
            throw IllegalArgumentException(
                "Given password seems to be not encrypted! Aborting due to security reasons (for avoiding storage of clear password in the database)."
            )
        }
    }

    /**
     * Checks the given password by comparing it with the stored user password. For backward compatibility the password is
     * encrypted with and without pepper (if configured). The salt string of the given user is used.
     *
     * @param user
     * @param clearTextPassword as clear text.
     * @return true if the password matches the user's password.
     */
    open fun checkPassword(user: PFUserDO?, clearTextPassword: CharArray): PasswordCheckResult? {
        if (user == null || user.id == null) {
            log.warn("User not given in checkPassword(PFUserDO, String) method.")
            return PasswordCheckResult.FAILED
        }
        val userId = user.id ?: return PasswordCheckResult.FAILED
        val passwords = internalGetByUserId(userId)
        if (passwords == null) {
            log.warn("Can't load user password for user '${user.username}' (${user.id}")
            return PasswordCheckResult.FAILED
        }
        if (passwords.passwordHash.isNullOrBlank()) {
            log.warn("User's password is blank, can't checkPassword(PFUserDO, String) for user '${user.username}' with id ${user.id}")
            return PasswordCheckResult.FAILED
        }
        val passwordHash = passwords.passwordHash
        val saltString = passwords.passwordSalt ?: ""
        var encryptedPassword = encryptAndClear(pepperString, saltString, clearTextPassword)
        if (passwordHash == encryptedPassword) {
            // Passwords match!
            if (saltString.isBlank()) {
                log.info("Password of user ${user.id} with username '${user.username}' is not yet salted!")
                return PasswordCheckResult.OK_WITHOUT_SALT
            }
            return PasswordCheckResult.OK
        }
        if (pepperString.isNotBlank()) {
            // Check password without pepper (if pepper was introduced after the user has set his/her password):
            encryptedPassword = encryptAndClear(null, saltString, clearTextPassword)
            if (passwordHash == encryptedPassword) {
                // Passwords match!
                if (saltString.isBlank()) {
                    log.info("Password of user ${user.id} with username '${user.username}' is not yet salted and has no pepper!")
                    return PasswordCheckResult.OK_WITHOUT_SALT_AND_PEPPER
                }
                log.info("Password of user ${user.id} with username '${user.username}' has no pepper!")
                return PasswordCheckResult.OK_WITHOUT_PEPPER
            }
        }
        return PasswordCheckResult.FAILED
    }


    /**
     * Creates salted and peppered password as char array, encrypts it and clears the char array afterwards due to security reasons.
     *
     * @param pepper
     * @param salt
     * @param password
     * @return
     */
    private fun encryptAndClear(pepper: String?, salt: String?, password: CharArray): String {
        val saltPepper = (pepper ?: "") + (salt ?: "")
        val saltedAndPepperedPassword = ArrayUtils.addAll(saltPepper.toCharArray(), *password)
        val encryptedPassword = digest(saltedAndPepperedPassword)
        LoginHandler.clearPassword(saltedAndPepperedPassword) // Clear array to to security reasons.
        return encryptedPassword
    }

    private lateinit var pepperString: String

    private val newSaltString: String
        get() = getSecureRandomAlphanumeric(10)
}
