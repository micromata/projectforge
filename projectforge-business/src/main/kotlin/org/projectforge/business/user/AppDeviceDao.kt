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

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.TimeUnit
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.web.WebUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * The installed apps of the users, see [AppDeviceDO].
 *
 * No [org.projectforge.framework.persistence.api.BaseDao], see [StayLoggedInTokenDao]: the token is the credential,
 * and the methods taking a user id restrict themselves to the rows of that user (the caller passes the logged-in
 * user, never a posted one).
 */
@Service
class AppDeviceDao {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /**
     * Days without any request after which the token of an app is invalid (sliding, like the stay-logged-in token).
     */
    @Value("\${projectforge.app.tokenExpiryDays:90}")
    internal var tokenExpiryDays: Int = 90

    /**
     * Registers a new app of the user.
     *
     * @return The device and the token in clear text: the app keeps it in the key chain, the database only its hash.
     */
    fun createDevice(
        user: PFUserDO,
        deviceName: String?,
        platform: String?,
        last2FA: Date?,
        request: HttpServletRequest?,
    ): Pair<AppDeviceDO, String> {
        val userId = requireNotNull(user.id) { "Can't register an app of a user without id." }
        val token = createToken()
        val entry = AppDeviceDO()
        entry.user = user
        entry.tokenHash = StayLoggedInTokenDao.hash(token)
        entry.deviceName = abbreviate(deviceName?.trim()?.ifBlank { null }, AppDeviceDO.MAX_DEVICE_NAME_LENGTH)
        entry.platform = abbreviate(platform?.trim()?.lowercase()?.ifBlank { null }, AppDeviceDO.MAX_PLATFORM_LENGTH)
        entry.created = Date()
        entry.lastAccess = entry.created
        entry.lastAccessIp = clientIp(request)
        entry.last2FA = last2FA
        persistenceService.runInTransaction { context ->
            context.em.persist(entry)
            context.em.flush()
        }
        log.info { "New app device #${entry.id} registered for user #$userId ('${user.username}'): ${entry.deviceName} (${entry.platform})." }
        return Pair(entry, token)
    }

    /**
     * A new login of an already registered app (its 2FA had expired): the device keeps its row (and later its push
     * token), but gets a new token and the time of the new second factor. The old token is invalid from now on.
     *
     * @param userId The logged-in user: a device of another user is never touched.
     * @return The new token, or null if the device isn't (or no longer) one of the user's.
     */
    fun renewDevice(userId: Long?, id: Long?, last2FA: Date?, request: HttpServletRequest?): String? {
        userId ?: return null
        id ?: return null
        val token = createToken()
        val found = persistenceService.runInTransaction { context ->
            val entry = context.selectNamedSingleResult(
                AppDeviceDO.FIND_BY_ID_AND_USER,
                AppDeviceDO::class.java,
                Pair("id", id),
                Pair("userId", userId),
            ) ?: return@runInTransaction false
            entry.tokenHash = StayLoggedInTokenDao.hash(token)
            entry.last2FA = last2FA
            entry.lastAccess = Date()
            entry.lastAccessIp = clientIp(request)
            context.em.merge(entry)
            context.em.flush()
            true
        }
        if (!found) {
            return null
        }
        log.info { "App device #$id of user #$userId renewed (new second factor)." }
        return token
    }

    /**
     * @return The device, or null if the token is unknown or expired. An expired row is deleted right here, see
     * [StayLoggedInTokenDao.getValidToken].
     */
    fun getValidDevice(token: String?): AppDeviceDO? {
        if (token.isNullOrBlank() || token.length < MIN_TOKEN_LENGTH) {
            return null
        }
        val entry = persistenceService.selectNamedSingleResult(
            AppDeviceDO.FIND_BY_TOKEN_HASH,
            AppDeviceDO::class.java,
            Pair("tokenHash", StayLoggedInTokenDao.hash(token)),
        ) ?: return null
        val lastAccess = entry.lastAccess
        if (lastAccess == null || lastAccess.before(expireBefore())) {
            log.info { "Token of app device #${entry.id} of user #${entry.user?.id} expired (last access: $lastAccess), deleting it." }
            deleteById(entry.id)
            return null
        }
        return entry
    }

    /**
     * Refreshes [AppDeviceDO.lastAccess], at most once per [REFRESH_THRESHOLD_MILLIS], see
     * [StayLoggedInTokenDao.updateLastAccessIfDue].
     */
    fun updateLastAccessIfDue(entry: AppDeviceDO, request: HttpServletRequest?) {
        val id = entry.id ?: return
        val now = System.currentTimeMillis()
        if (now - (entry.lastAccess?.time ?: 0) < REFRESH_THRESHOLD_MILLIS) {
            return
        }
        val date = Date(now)
        val appVersion = abbreviate(request?.getHeader(APP_VERSION_HEADER), AppDeviceDO.MAX_APP_VERSION_LENGTH)
        persistenceService.runInTransaction { context ->
            context.executeNamedUpdate(
                AppDeviceDO.UPDATE_LAST_ACCESS,
                Pair("lastAccess", date),
                Pair("lastAccessIp", clientIp(request)),
                Pair("appVersion", appVersion ?: entry.appVersion),
                Pair("id", id),
            )
        }
        entry.lastAccess = date
    }

    /**
     * The devices of a user, most recently used first. For displaying them to their owner.
     */
    fun getEntries(userId: Long?): List<AppDeviceDO> {
        userId ?: return emptyList()
        return persistenceService.executeNamedQuery(
            AppDeviceDO.FIND_BY_USER,
            AppDeviceDO::class.java,
            Pair("userId", userId),
        )
    }

    /**
     * @return The device, if it is one of the given user's, otherwise null.
     */
    fun getOfUser(userId: Long?, id: Long?): AppDeviceDO? {
        userId ?: return null
        id ?: return null
        return persistenceService.selectNamedSingleResult(
            AppDeviceDO.FIND_BY_ID_AND_USER,
            AppDeviceDO::class.java,
            Pair("id", id),
            Pair("userId", userId),
        )
    }

    /**
     * Logs out one app of a user. The user is part of the condition, so an id of another user's device deletes
     * nothing.
     *
     * @return 1, or 0 if the device isn't (or no longer) one of the user's.
     */
    fun deleteOfUser(userId: Long?, id: Long?): Int {
        userId ?: return 0
        id ?: return 0
        val counter = persistenceService.runInTransaction { context ->
            context.executeNamedUpdate(AppDeviceDO.DELETE_BY_ID_AND_USER, Pair("id", id), Pair("userId", userId))
        }
        if (counter > 0) {
            log.info { "App device #$id of user #$userId deleted (logged out)." }
        }
        return counter
    }

    /**
     * Logs out all apps of a user: password change, "log out everywhere", admin reset.
     */
    fun deleteAll(userId: Long?): Int {
        userId ?: return 0
        val counter = persistenceService.runInTransaction { context ->
            context.executeNamedUpdate(AppDeviceDO.DELETE_BY_USER, Pair("userId", userId))
        }
        if (counter > 0) {
            log.info { "All $counter app device(s) of user #$userId deleted." }
        }
        return counter
    }

    /**
     * Housekeeping (nightly job), the expiry itself is enforced by [getValidDevice].
     */
    fun purgeExpired(): Int {
        val counter = persistenceService.runInTransaction { context ->
            context.executeNamedUpdate(AppDeviceDO.DELETE_EXPIRED, Pair("expireBefore", expireBefore()))
        }
        if (counter > 0) {
            log.info { "$counter expired app device(s) purged." }
        }
        return counter
    }

    private fun deleteById(id: Long?) {
        id ?: return
        persistenceService.runInTransaction { context ->
            context.em.find(AppDeviceDO::class.java, id)?.let { attached ->
                context.em.remove(attached)
                context.em.flush()
            }
        }
    }

    private fun expireBefore(): Date {
        return Date(System.currentTimeMillis() - tokenExpiryDays * TimeUnit.DAY.millis)
    }

    private fun clientIp(request: HttpServletRequest?): String? {
        request ?: return null
        return abbreviate(WebUtils.getClientIp(request), StayLoggedInTokenDO.MAX_IP_LENGTH)
    }

    private fun abbreviate(value: String?, maxLength: Int): String? {
        value ?: return null
        return if (value.length > maxLength) value.substring(0, maxLength) else value
    }

    companion object {
        /**
         * Header with the version of the app, see [AppDeviceDO.appVersion].
         */
        const val APP_VERSION_HEADER = "X-PF-App-Version"

        /**
         * 40 chars of a 58 char alphabet, roughly 234 bits. Never displayed, see [StayLoggedInTokenDao].
         */
        private const val TOKEN_LENGTH = 40

        private const val MIN_TOKEN_LENGTH = 20

        private val REFRESH_THRESHOLD_MILLIS = TimeUnit.HOUR.millis

        private fun createToken(): String = NumberHelper.getSecureRandomAlphanumeric(TOKEN_LENGTH)
    }
}
