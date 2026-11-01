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
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.TimeUnit
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.security.ConstantTimeCompare
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

/**
 * Login of the mobile app, an authorization code flow with PKCE (RFC 7636) and without a client secret:
 *
 * 1. The app opens `/next/appLogin?challenge=…&state=…` in the system browser. The user logs in there as usual
 *    (password, WebAuthn or OTP), does a fresh second factor and confirms the app.
 * 2. The page calls [authorize] (session, CSRF token) and is redirected to [APP_REDIRECT_URL] with a one-time code.
 * 3. The app exchanges the code and its code verifier for a token of its own ([exchange]).
 *
 * The redirect target is fixed: another app may register the same url scheme and catch the code, but without the
 * verifier, which never left the app, the code is worthless.
 *
 * The codes live in memory only: they are valid for [CODE_EXPIRY_MILLIS], a restart just makes the user log in once
 * again.
 */
@Service
class AppAuthService {
    @Autowired
    private lateinit var appDeviceDao: AppDeviceDao

    @Autowired
    private lateinit var userDao: UserDao

    private class PendingCode(
        val userId: Long,
        val challenge: String,
        val deviceName: String?,
        val platform: String?,
        val renewDeviceId: Long?,
        val last2FA: Long,
        val expiresAt: Long,
    )

    class Exchanged(val user: PFUserDO, val device: AppDeviceDO, val token: String)

    private val pendingCodes = ConcurrentHashMap<String, PendingCode>()

    /**
     * @param user The logged-in user (of the session, never a posted one).
     * @param lastSuccessful2FA The last second factor of the session, checked by the caller to be fresh.
     * @param renewDeviceId An app already registered asks for a new second factor: its row is kept (with a new
     * token), if it belongs to [user]; otherwise a new device is registered.
     * @return The url the browser is redirected to (the app's callback with the code), or null for an invalid
     * challenge or state.
     */
    fun authorize(
        user: PFUserDO,
        lastSuccessful2FA: Long,
        challenge: String?,
        state: String?,
        deviceName: String?,
        platform: String?,
        renewDeviceId: Long?,
    ): String? {
        val userId = user.id ?: return null
        if (challenge == null || !CHALLENGE_REGEX.matches(challenge) || state == null || !STATE_REGEX.matches(state)) {
            log.warn { "App authorization of user #$userId refused: invalid challenge or state." }
            return null
        }
        purgeExpiredCodes()
        val renewId = renewDeviceId?.takeIf { appDeviceDao.getOfUser(userId, it) != null }
        val code = NumberHelper.getSecureRandomAlphanumeric(CODE_LENGTH)
        pendingCodes[code] = PendingCode(
            userId = userId,
            challenge = challenge,
            deviceName = deviceName,
            platform = platform,
            renewDeviceId = renewId,
            last2FA = lastSuccessful2FA,
            expiresAt = System.currentTimeMillis() + CODE_EXPIRY_MILLIS,
        )
        log.info { "App authorization code issued for user #$userId (device: $deviceName, renew: $renewId)." }
        return "$APP_REDIRECT_URL?code=${encode(code)}&state=${encode(state)}"
    }

    /**
     * Exchanges a code issued by [authorize]. The code is consumed by the first attempt, whether successful or not.
     *
     * @return The app's device and token, or null if the code is unknown, expired or the verifier doesn't match.
     */
    fun exchange(code: String?, codeVerifier: String?, request: HttpServletRequest?): Exchanged? {
        code ?: return null
        val pending = pendingCodes.remove(code) ?: return null
        if (pending.expiresAt < System.currentTimeMillis()) {
            log.info { "App authorization code of user #${pending.userId} expired." }
            return null
        }
        if (codeVerifier == null || !VERIFIER_REGEX.matches(codeVerifier) ||
            !ConstantTimeCompare.equals(pending.challenge, s256(codeVerifier))
        ) {
            log.warn { "App authorization code of user #${pending.userId} refused: code verifier doesn't match." }
            return null
        }
        val user = userDao.find(pending.userId, checkAccess = false)
        if (user == null || !user.hasSystemAccess()) {
            log.warn { "App authorization refused: user #${pending.userId} not found or without system access." }
            return null
        }
        val last2FA = Date(pending.last2FA)
        pending.renewDeviceId?.let { deviceId ->
            appDeviceDao.renewDevice(pending.userId, deviceId, last2FA, request)?.let { token ->
                appDeviceDao.getOfUser(pending.userId, deviceId)?.let { device ->
                    return Exchanged(user, device, token)
                }
            }
        }
        val (device, token) = appDeviceDao.createDevice(user, pending.deviceName, pending.platform, last2FA, request)
        return Exchanged(user, device, token)
    }

    private fun purgeExpiredCodes() {
        val now = System.currentTimeMillis()
        pendingCodes.entries.removeIf { it.value.expiresAt < now }
    }

    internal fun clearAll4UnitTests() {
        pendingCodes.clear()
    }

    companion object {
        /**
         * The callback of the app (its url scheme, see projectforge-flutter).
         */
        const val APP_REDIRECT_URL = "projectforge://auth"

        /**
         * Maximum age of the second factor for issuing a token.
         */
        const val MAX_2FA_AGE_MINUTES = 10L

        private const val CODE_LENGTH = 32

        private val CODE_EXPIRY_MILLIS = 2 * TimeUnit.MINUTE.millis

        /** Base64url of a SHA-256 without padding. */
        private val CHALLENGE_REGEX = Regex("^[A-Za-z0-9_-]{43}$")

        /** RFC 7636, section 4.1. */
        private val VERIFIER_REGEX = Regex("^[A-Za-z0-9._~-]{43,128}$")

        private val STATE_REGEX = Regex("^[A-Za-z0-9_-]{16,128}$")

        internal fun s256(codeVerifier: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray(StandardCharsets.US_ASCII))
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        }

        private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
    }
}
