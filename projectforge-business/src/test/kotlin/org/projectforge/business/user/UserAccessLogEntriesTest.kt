/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserAccessLogEntriesTest {
    @Test
    fun testUserAccessLogs() {
        val entries = UserAccessLogEntries(UserTokenType.REST_CLIENT)
        entries.update(userAgent = "iOS", ip = "127.0.0.1")
        assertEquals(1, entries.size())
        entries.update(userAgent = "iOS", ip = "127.0.0.1")
        assertEquals(1, entries.size())
        Thread.sleep(2)
        entries.update(userAgent = "MacOS X", ip = "127.0.0.1")
        assertEquals(2, entries.size())
        entries.update(userAgent = "MacOS X", ip = "127.0.0.2")
        assertEquals(3, entries.size())
        for (i in 0..16) {
            entries.update(userAgent = "MacOS X", ip = "192.168.0.$i")
        }
        assertEquals(20, entries.size())
        assertTrue(entries.sortedList().any { it.userAgent == "iOS" })
        entries.update(userAgent = "Debian Linux", ip = "127.0.0.1")
        assertEquals(20, entries.size())
        assertTrue(entries.sortedList().none { it.userAgent == "iOS" }, "iOS entry as oldest entry should be removed now.")
    }
}
