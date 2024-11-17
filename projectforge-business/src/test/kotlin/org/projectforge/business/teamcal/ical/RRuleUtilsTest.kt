/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.teamcal.ical

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.common.extensions.isoUTCString
import org.projectforge.test.TestSetup

class RRuleUtilsTest {
    @Test
    fun `test parsing ex dates`() {
        RRuleUtils.parseExcludeDates("EXDATE:2024-11-14T09:00:00Z,2024-11-15T09:00:00Z")?.let { exDates ->
            Assertions.assertEquals(2, exDates.size)
            Assertions.assertEquals("2024-11-14T09:00:00Z", exDates[0].isoUTCString())
            Assertions.assertEquals("2024-11-15T09:00:00Z", exDates[1].isoUTCString())
        }
        RRuleUtils.parseExcludeDates("2024-11-14,2024-11-15T09:00:00Z")?.let { exDates ->
            Assertions.assertEquals(2, exDates.size)
            Assertions.assertEquals("2024-11-14", exDates[0].isoUTCString())
            Assertions.assertEquals("2024-11-15T09:00:00Z", exDates[1].isoUTCString())
        }
        RRuleUtils.parseExcludeDates("20130328T200000")?.let { exDates ->
            Assertions.assertEquals(1, exDates.size)
            Assertions.assertEquals("2013-03-28T20:00:00", exDates[0].isoUTCString())
        }
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestSetup.init()
        }
    }
}
