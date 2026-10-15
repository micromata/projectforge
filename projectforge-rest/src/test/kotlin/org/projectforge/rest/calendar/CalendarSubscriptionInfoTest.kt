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

package org.projectforge.rest.calendar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.business.teamcal.CalendarAccessStatus
import org.projectforge.business.test.AbstractTestBase

/**
 * Reminders are exported by default for everyone who may see the full events, as the Wicket dialog did.
 */
class CalendarSubscriptionInfoTest : AbstractTestBase() {
    @Test
    fun `reminders default follows the access status`() {
        mapOf(
            CalendarAccessStatus.OWNER to true,
            CalendarAccessStatus.FULL_ACCESS to true,
            CalendarAccessStatus.READONLY_ACCESS to true,
            CalendarAccessStatus.MINIMAL_ACCESS to false,
            CalendarAccessStatus.ADMIN_ACCESS to false,
        ).forEach { (status, expected) ->
            assertEquals(expected, CalendarSubscriptionInfo(accessStatus = status).remindersExportDefaultValue, "$status")
        }
        assertEquals(false, CalendarSubscriptionInfo().remindersExportDefaultValue)
    }
}
