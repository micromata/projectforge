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

package org.projectforge.framework.persistence.jpa

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MaintenanceDbTimeoutsTest {
    @AfterEach
    fun reset() {
        MaintenanceDbTimeouts.lockTimeoutSeconds = MaintenanceDbTimeouts.DEFAULT_LOCK_TIMEOUT_SECONDS
        MaintenanceDbTimeouts.statementTimeoutSeconds = 0
    }

    @Test
    fun `the scope is nestable and limited to its block`() {
        Assertions.assertFalse(MaintenanceDbTimeouts.isActive)
        MaintenanceDbTimeouts.run {
            Assertions.assertTrue(MaintenanceDbTimeouts.isActive)
            MaintenanceDbTimeouts.run { Assertions.assertTrue(MaintenanceDbTimeouts.isActive) }
            Assertions.assertTrue(MaintenanceDbTimeouts.isActive, "Still active after the nested block.")
        }
        Assertions.assertFalse(MaintenanceDbTimeouts.isActive)
        Assertions.assertThrows(IllegalStateException::class.java) {
            MaintenanceDbTimeouts.run { throw IllegalStateException("test") }
        }
        Assertions.assertFalse(MaintenanceDbTimeouts.isActive, "Ended by an exception, too.")
    }

    @Test
    fun `settings of both dialects`() {
        Assertions.assertEquals(listOf("lock_timeout" to "60s"), MaintenanceDbTimeouts.settings())
        Assertions.assertEquals(60_000, MaintenanceDbTimeouts.queryTimeoutMillis())
        MaintenanceDbTimeouts.statementTimeoutSeconds = 30
        Assertions.assertEquals(
            listOf("lock_timeout" to "60s", "statement_timeout" to "30s"),
            MaintenanceDbTimeouts.settings(),
        )
        Assertions.assertEquals(30_000, MaintenanceDbTimeouts.queryTimeoutMillis(), "HSQLDB: the smaller one.")
        MaintenanceDbTimeouts.lockTimeoutSeconds = 0
        MaintenanceDbTimeouts.statementTimeoutSeconds = 0
        Assertions.assertTrue(MaintenanceDbTimeouts.settings().isEmpty())
        Assertions.assertNull(MaintenanceDbTimeouts.queryTimeoutMillis())
    }
}
