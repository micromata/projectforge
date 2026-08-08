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

package org.projectforge.business.user

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.TimeUnit
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.web.WebUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * One row per device, see [StayLoggedInTokenDO].
 *
 * No [org.projectforge.framework.persistence.api.BaseDao]: every method here runs before the login (the
 * cookie is checked by the servlet filters) or during the logout, so there is no logged-in user an access
 * check could refer to. The authorization is the token itself - knowing it *is* the credential.
 */
@Service
class StayLoggedInTokenDao {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /**
     * Creates a token for one device and stores its hash.
     *
     * @return The token in clear text. It is only ever stored in the user's cookie: the database holds the
     * hash, so a database dump doesn't yield usable credentials.
     */
    fun createToken(user: PFUserDO, request: HttpServletRequest?): String {
        val userId = requireNotNull(user.id) { "Can't create a stay-logged-in token for a user without id." }
        val token = NumberHelper.getSecureRandomAlphanumeric(TOKEN_LENGTH)
        val entry = StayLoggedInTokenDO()
        entry.user = user
        entry.tokenHash = hash(token)
        entry.created = Date()
        entry.lastAccess = entry.created
        entry.lastAccessIp = clientIp(request)
        entry.userAgent = userAgent(request)
        persistenceService.runInTransaction { context ->
            context.em.persist(entry)
            context.em.flush()
        }
        log.info { "New stay-logged-in token created for user #$userId ('${user.username}'), device: ${entry.userAgent}." }
        return token
    }

    /**
     * @return The device's row, or null if the token is unknown or expired. An expired row is deleted right
     * here: the expiry must not depend on the purge job having run.
     */
    fun getValidToken(token: String?): StayLoggedInTokenDO? {
        if (token.isNullOrBlank() || token.length < MIN_TOKEN_LENGTH) {
            return null // Not a token this class ever created, so don't even ask the database.
        }
        val entry = persistenceService.selectNamedSingleResult(
            StayLoggedInTokenDO.FIND_BY_TOKEN_HASH,
            StayLoggedInTokenDO::class.java,
            Pair("tokenHash", hash(token)),
        ) ?: return null
        // Sliding window on lastAccess, not on created: the cookie's max age is refreshed on every
        // successful check, so anchoring on created would log active users out after 30 days.
        val expireBefore = expireBefore()
        val lastAccess = entry.lastAccess
        if (lastAccess == null || lastAccess.before(expireBefore)) {
            log.info { "Stay-logged-in token of user #${entry.user?.id} expired (last access: $lastAccess), deleting it." }
            deleteById(entry.id)
            return null
        }
        return entry
    }

    /**
     * Refreshes [StayLoggedInTokenDO.lastAccess] (and the device info shown to the user), but at most once
     * per [REFRESH_THRESHOLD_MILLIS]: a page load is a whole wave of requests, and each one carries the
     * cookie.
     */
    fun updateLastAccessIfDue(entry: StayLoggedInTokenDO, request: HttpServletRequest?) {
        val id = entry.id ?: return
        val now = System.currentTimeMillis()
        val lastAccess = entry.lastAccess?.time ?: 0
        if (now - lastAccess < REFRESH_THRESHOLD_MILLIS) {
            return
        }
        val date = Date(now)
        persistenceService.runInTransaction { context ->
            context.executeNamedUpdate(
                StayLoggedInTokenDO.UPDATE_LAST_ACCESS,
                Pair("lastAccess", date),
                Pair("lastAccessIp", clientIp(request)),
                Pair("userAgent", userAgent(request)),
                Pair("id", id),
            )
        }
        entry.lastAccess = date
    }

    /**
     * Invalidates one device (logout).
     */
    fun deleteByToken(token: String?): Int {
        if (token.isNullOrBlank()) {
            return 0
        }
        return persistenceService.runInTransaction { context ->
            context.executeNamedUpdate(StayLoggedInTokenDO.DELETE_BY_TOKEN_HASH, Pair("tokenHash", hash(token)))
        }
    }

    /**
     * Invalidates all devices of a user: password change, "log out everywhere", admin reset.
     */
    fun deleteAll(userId: Long?): Int {
        userId ?: return 0
        val counter = persistenceService.runInTransaction { context ->
            context.executeNamedUpdate(StayLoggedInTokenDO.DELETE_BY_USER, Pair("userId", userId))
        }
        if (counter > 0) {
            log.info { "All $counter stay-logged-in token(s) of user #$userId deleted." }
        }
        return counter
    }

    /**
     * The devices of a user, most recently used first. For displaying them to their owner.
     */
    fun getEntries(userId: Long?): List<StayLoggedInTokenDO> {
        userId ?: return emptyList()
        return persistenceService.executeNamedQuery(
            StayLoggedInTokenDO.FIND_BY_USER,
            StayLoggedInTokenDO::class.java,
            Pair("userId", userId),
        )
    }

    /**
     * Housekeeping of rows nobody will ever use again (called by the nightly job). Not the enforcement of
     * the expiry, that happens in [getValidToken].
     */
    fun purgeExpired(): Int {
        val counter = persistenceService.runInTransaction { context ->
            context.executeNamedUpdate(StayLoggedInTokenDO.DELETE_EXPIRED, Pair("expireBefore", expireBefore()))
        }
        if (counter > 0) {
            log.info { "$counter expired stay-logged-in token(s) purged." }
        }
        return counter
    }

    private fun deleteById(id: Long?) {
        id ?: return
        persistenceService.runInTransaction { context ->
            context.em.find(StayLoggedInTokenDO::class.java, id)?.let { attached ->
                context.em.remove(attached)
                context.em.flush()
            }
        }
    }

    private fun expireBefore(): Date {
        return Date(System.currentTimeMillis() - EXPIRY_MILLIS)
    }

    private fun clientIp(request: HttpServletRequest?): String? {
        request ?: return null
        return abbreviate(WebUtils.getClientIp(request), StayLoggedInTokenDO.MAX_IP_LENGTH)
    }

    private fun userAgent(request: HttpServletRequest?): String? {
        request ?: return null
        return abbreviate(request.getHeader("User-Agent"), StayLoggedInTokenDO.MAX_USER_AGENT_LENGTH)
    }

    private fun abbreviate(value: String?, maxLength: Int): String? {
        value ?: return null
        return if (value.length > maxLength) value.substring(0, maxLength) else value
    }

    companion object {
        /**
         * 32 chars of a 58 char alphabet, roughly 187 bits. Much more than the 19 chars of the other token
         * types ([UserAuthenticationsDao.createAuthenticationToken]): those are grouped by dashes so a human
         * can copy them from the UI, but a stay-logged-in token is never displayed.
         */
        private const val TOKEN_LENGTH = 32

        /**
         * Nothing shorter can be a token of ours, so a garbage cookie doesn't cost a database roundtrip.
         */
        private const val MIN_TOKEN_LENGTH = 20

        /**
         * Has to be the max age of the cookie
         * ([org.projectforge.business.user.filter.CookieService.COOKIE_STAY_LOGGED_IN_MAX_AGE]): both are
         * refreshed on every successful check.
         */
        private val EXPIRY_MILLIS = 30 * TimeUnit.DAY.millis

        /**
         * Don't write lastAccess more often than this.
         */
        private val REFRESH_THRESHOLD_MILLIS = TimeUnit.HOUR.millis

        /**
         * Lowercase hex SHA-256.
         *
         * Not [org.projectforge.framework.utils.Crypt.digest]: that one is SHA-1 and wraps its result in
         * `SHA{...}`. Not [org.projectforge.framework.utils.Crypt.encrypt] either - encryption is
         * reversible, and nothing needs to read this value back.
         */
        internal fun hash(token: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
