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

package org.projectforge.rest.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SupportErrorMailerTest {
    @Test
    fun `the same error passes the throttle once per interval`() {
        var now = 0L
        val throttle = SupportErrorMailer.Throttle(1000L) { now }
        Assertions.assertTrue(throttle.acquire("a"))
        Assertions.assertFalse(throttle.acquire("a"), "Same key within the interval.")
        Assertions.assertTrue(throttle.acquire("b"), "Another key is independent.")
        now = 999L
        Assertions.assertFalse(throttle.acquire("a"))
        now = 1000L
        Assertions.assertTrue(throttle.acquire("a"), "Interval passed.")
        Assertions.assertFalse(throttle.acquire("a"))
    }

    @Test
    fun `the throttle stays bounded`() {
        var now = 0L
        val throttle = SupportErrorMailer.Throttle(1000L) { now }
        repeat(1000) { Assertions.assertTrue(throttle.acquire("key$it")) }
        // Full and nothing expired: everything is forgotten, so a known key passes again.
        Assertions.assertTrue(throttle.acquire("key0"))
        now = 500L
        Assertions.assertFalse(throttle.acquire("key0"))
    }

    @Test
    fun `the key is class and message`() {
        Assertions.assertEquals(
            "java.lang.IllegalStateException: boom",
            SupportErrorMailer.throttleKey(IllegalStateException("boom")),
        )
        Assertions.assertNotEquals(
            SupportErrorMailer.throttleKey(IllegalStateException("boom")),
            SupportErrorMailer.throttleKey(IllegalArgumentException("boom")),
        )
    }

    @Test
    fun `known exceptions exempted from mails are not mailed`() {
        // The gate report() starts with: unknown errors are mailed, known ones only if registered so.
        Assertions.assertTrue(GlobalExceptionRegistry.sendMailToDevelopers(NullPointerException()))
        Assertions.assertFalse(GlobalExceptionRegistry.sendMailToDevelopers(IllegalStateException("Cannot start async")))
    }
}
