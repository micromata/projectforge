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

package org.projectforge.business.fibu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrderNotificationPrefsTest {
    @Test
    fun `without rules the default applies`() {
        assertEquals(OrderNotificationMode.IMMEDIATE, OrderNotificationPrefs().resolve(1, 2))
        assertEquals(
            OrderNotificationMode.DAILY,
            OrderNotificationPrefs(OrderNotificationMode.DAILY).resolve(null, null),
        )
    }

    @Test
    fun `project rule wins over customer rule, which wins over the default`() {
        val prefs = OrderNotificationPrefs(
            defaultMode = OrderNotificationMode.DAILY,
            projectModes = mutableMapOf(1L to OrderNotificationMode.MUTED),
            customerModes = mutableMapOf(1L to OrderNotificationMode.IMMEDIATE, 2L to OrderNotificationMode.MUTED),
        )
        assertEquals(OrderNotificationMode.MUTED, prefs.resolve(projectId = 1, 1))
        assertEquals(OrderNotificationMode.IMMEDIATE, prefs.resolve(projectId = 5, 1))
        assertEquals(OrderNotificationMode.MUTED, prefs.resolve(projectId = null, 2))
        assertEquals(OrderNotificationMode.DAILY, prefs.resolve(projectId = 5, 3))
    }

    @Test
    fun `a rule for the customer of the project applies if the order has another customer`() {
        val prefs = OrderNotificationPrefs(customerModes = mutableMapOf(2L to OrderNotificationMode.MUTED))
        assertEquals(OrderNotificationMode.MUTED, prefs.resolve(null, 1L, 2L))
        assertEquals(OrderNotificationMode.IMMEDIATE, prefs.resolve(null, 1L, 3L))
    }
}
