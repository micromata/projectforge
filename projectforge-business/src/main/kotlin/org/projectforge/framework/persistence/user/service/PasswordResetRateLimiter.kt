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


package org.projectforge.framework.persistence.user.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.framework.time.TimeUnit
import org.projectforge.web.TrustedProxies

private val log = KotlinLogging.logger {}

/**
 * Limits the number of password reset mails (mail bombing, abuse of the mail server and invalidating the token of
 * a user by requesting a new one again and again, see [PasswordResetTokenStore.createToken]).
 *
 * - [tryAcquireClient] counts every request (per client ip and globally), whatever the given username or mail is, so
 *   a refusal tells nothing about the existence of a user.
 * - [tryAcquireUser] is only checked in the mail thread of [PasswordResetService.sendMail], so it doesn't change the
 *   answer of the request either: a refused mail is silently dropped.
 *
 * The limit per user is an hourly one on purpose: an attacker knowing the username could otherwise block the reset
 * mails of a user for a whole day with a few requests. The global limit only catches real floods, because it is a
 * lever for blocking the password reset of all users.
 *
 * The maps are bounded: a client ip or user is only recorded, if the global limit allows the request.
 */
internal object PasswordResetRateLimiter {
    /**
     * Clock, replaceable by tests.
     */
    internal var now: () -> Long = System::currentTimeMillis

    private val byUser = SlidingWindowCounter<Long>(
        Limit(2 * TimeUnit.MINUTE.millis, 1),
        Limit(TimeUnit.HOUR.millis, 5),
    )

    private val byClient = SlidingWindowCounter<String>(Limit(TimeUnit.HOUR.millis, 10))

    private val global = SlidingWindowCounter<Unit>(Limit(TimeUnit.HOUR.millis, 1000))

    private var globalLimitLoggedMillis = 0L

    /**
     * Records the request, if allowed.
     * @param clientIp The ip address of the client, if known (otherwise only the global limit is checked). IPv6
     * addresses are counted per /64 network, see [clientKey].
     * @return false, if the global limit or the limit of the given client ip is exceeded.
     */
    fun tryAcquireClient(clientIp: String?): Boolean {
        val clientKey = clientKey(clientIp)
        synchronized(this) {
            val millis = now()
            tidyUp(millis)
            if (!global.isAllowed(Unit, millis)) {
                if (millis - globalLimitLoggedMillis > TimeUnit.HOUR.millis) {
                    globalLimitLoggedMillis = millis
                    log.error { "Global limit of password reset requests exceeded, further requests are refused (an attack?)." }
                }
                return false
            }
            if (clientKey != null && !byClient.isAllowed(clientKey, millis)) {
                return false
            }
            global.record(Unit, millis)
            clientKey?.let { byClient.record(it, millis) }
            return true
        }
    }

    /**
     * Records the mail, if allowed.
     * @return false, if the limit of mails for the given user is exceeded.
     */
    fun tryAcquireUser(userId: Long): Boolean {
        synchronized(this) {
            val millis = now()
            tidyUp(millis)
            if (!byUser.isAllowed(userId, millis)) {
                return false
            }
            byUser.record(userId, millis)
            return true
        }
    }

    fun clearAll() {
        synchronized(this) {
            byUser.clear()
            byClient.clear()
            global.clear()
            globalLimitLoggedMillis = 0L
        }
    }

    private fun tidyUp(millis: Long) {
        byUser.tidyUp(millis)
        byClient.tidyUp(millis)
        global.tidyUp(millis)
    }

    internal fun size(): Int {
        synchronized(this) {
            return byUser.size + byClient.size + global.size
        }
    }

    /**
     * A client usually gets a whole IPv6 /64 network, so all of its addresses share one counter. IPv4 addresses (and
     * unparseable values) are used as they are.
     */
    internal fun clientKey(clientIp: String?): String? {
        clientIp ?: return null
        val bytes = TrustedProxies.parseAddress(clientIp)
        if (bytes == null || bytes.size != 16) {
            return clientIp
        }
        return bytes.take(8).chunked(2).joinToString(":", postfix = "::/64") {
            "%x".format(((it[0].toInt() and 0xff) shl 8) or (it[1].toInt() and 0xff))
        }
    }

    private class Limit(val windowMillis: Long, val maxCount: Int)

    /**
     * Timestamps of the recorded events per key. Not thread safe (synchronized by [PasswordResetRateLimiter]).
     */
    private class SlidingWindowCounter<K : Any>(vararg val limits: Limit) {
        private val map = mutableMapOf<K, ArrayDeque<Long>>()
        private val maxWindowMillis = limits.maxOf { it.windowMillis }

        val size: Int
            get() = map.size

        fun isAllowed(key: K, millis: Long): Boolean {
            val timestamps = map[key] ?: return true
            return limits.all { limit ->
                timestamps.count { millis - it < limit.windowMillis } < limit.maxCount
            }
        }

        fun record(key: K, millis: Long) {
            map.getOrPut(key) { ArrayDeque() }.addLast(millis)
        }

        fun clear() {
            map.clear()
        }

        fun tidyUp(millis: Long) {
            map.values.forEach { timestamps ->
                while (timestamps.isNotEmpty() && millis - timestamps.first() >= maxWindowMillis) {
                    timestamps.removeFirst()
                }
            }
            map.values.removeIf { it.isEmpty() }
        }
    }
}
