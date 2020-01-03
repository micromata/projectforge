/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.time

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.framework.time.PFDateTime.Companion.from
import org.projectforge.test.TestSetup
import java.time.Month

class PFDateTimeUtilsTest {
    @Test
    fun testMidnightCalendars() {
        val timeZone = PFDateTimeUtils.TIMEZONE_EUROPE_BERLIN
        val testDate = PFDateTime.withDate(2012, Month.DECEMBER, 23, 8, 33, 24, 123, timeZone.toZoneId());
        val utcDate: PFDateTime = PFDateTimeUtils.getUTCBeginOfDay(testDate.utilDate, timeZone)
        Assertions.assertEquals("2012-12-23 00:00:00.000", utcDate.isoStringMilli)
        Assertions.assertEquals("2012-12-22 23:00:00.000", from(testDate.utilDate)!!.beginOfDay.isoStringMilli)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestSetup.init()
        }
    }
}
