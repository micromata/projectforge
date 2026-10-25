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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.framework.time.TimeUnit

class PasswordResetRateLimiterTest {
    private var millis = 1_000_000_000L

    @BeforeEach
    fun setUp() {
        PasswordResetRateLimiter.clearAll()
        PasswordResetRateLimiter.now = { millis }
    }

    @AfterEach
    fun tearDown() {
        PasswordResetRateLimiter.clearAll()
        PasswordResetRateLimiter.now = System::currentTimeMillis
    }

    @Test
    fun `one mail per user within 2 minutes, at most 5 per hour`() {
        Assertions.assertTrue(PasswordResetRateLimiter.tryAcquireUser(1))
        Assertions.assertFalse(PasswordResetRateLimiter.tryAcquireUser(1))
        Assertions.assertTrue(PasswordResetRateLimiter.tryAcquireUser(2), "Other users aren't affected.")
        millis += 2 * TimeUnit.MINUTE.millis
        Assertions.assertTrue(PasswordResetRateLimiter.tryAcquireUser(1))
        repeat(3) {
            millis += 2 * TimeUnit.MINUTE.millis
            Assertions.assertTrue(PasswordResetRateLimiter.tryAcquireUser(1))
        }
        millis += 2 * TimeUnit.MINUTE.millis
        Assertions.assertFalse(PasswordResetRateLimiter.tryAcquireUser(1), "6th mail within an hour.")
        millis += TimeUnit.HOUR.millis
        Assertions.assertTrue(PasswordResetRateLimiter.tryAcquireUser(1))
    }

    @Test
    fun `10 requests per client and hour`() {
        repeat(10) {
            Assertions.assertTrue(PasswordResetRateLimiter.tryAcquireClient("10.0.0.1"))
        }
        Assertions.assertFalse(PasswordResetRateLimiter.tryAcquireClient("10.0.0.1"))
        Assertions.assertTrue(PasswordResetRateLimiter.tryAcquireClient("10.0.0.2"), "Other clients aren't affected.")
        millis += TimeUnit.HOUR.millis
        Assertions.assertTrue(PasswordResetRateLimiter.tryAcquireClient("10.0.0.1"))
    }

    @Test
    fun `global limit of 1000 requests per hour`() {
        repeat(1000) {
            Assertions.assertTrue(PasswordResetRateLimiter.tryAcquireClient("10.0.${it / 10}.${it % 10}"))
        }
        Assertions.assertFalse(PasswordResetRateLimiter.tryAcquireClient("10.1.0.1"))
        Assertions.assertFalse(PasswordResetRateLimiter.tryAcquireClient(null))
        millis += TimeUnit.HOUR.millis
        Assertions.assertTrue(PasswordResetRateLimiter.tryAcquireClient(null))
    }

    @Test
    fun `addresses of one IPv6 network share a counter`() {
        Assertions.assertEquals("2001:db8:0:1::/64", PasswordResetRateLimiter.clientKey("2001:db8:0:1:aaaa::1"))
        Assertions.assertEquals("10.0.0.1", PasswordResetRateLimiter.clientKey("10.0.0.1"))
        Assertions.assertEquals("no-ip", PasswordResetRateLimiter.clientKey("no-ip"))
        repeat(10) {
            Assertions.assertTrue(PasswordResetRateLimiter.tryAcquireClient("2001:db8:0:1::${it + 1}"))
        }
        Assertions.assertFalse(PasswordResetRateLimiter.tryAcquireClient("[2001:db8:0:1:ffff::42]"))
        Assertions.assertTrue(PasswordResetRateLimiter.tryAcquireClient("2001:db8:0:2::1"), "Other network.")
    }

    @Test
    fun `a valid token is reused instead of replaced`() {
        val token = PasswordResetTokenStore.createToken(4711)
        try {
            Assertions.assertEquals(token, PasswordResetTokenStore.createToken(4711))
            Assertions.assertEquals(4711L, PasswordResetTokenStore.checkToken(token))
            PasswordResetTokenStore.deleteToken(token)
            Assertions.assertNotEquals(token, PasswordResetTokenStore.createToken(4711), "New token after deletion.")
        } finally {
            PasswordResetTokenStore.deleteToken(PasswordResetTokenStore.createToken(4711)) // The current one.
        }
    }

    @Test
    fun `expired entries are removed`() {
        PasswordResetRateLimiter.tryAcquireClient("10.0.0.1")
        PasswordResetRateLimiter.tryAcquireUser(1)
        Assertions.assertEquals(3, PasswordResetRateLimiter.size())
        millis += TimeUnit.DAY.millis
        PasswordResetRateLimiter.tryAcquireUser(2) // tidies up all counters
        Assertions.assertEquals(1, PasswordResetRateLimiter.size(), "Only user 2 is left.")
    }
}
