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
import java.time.ZoneId

class PFDateTimeUtilsTest {
    @Test
    fun testMidnightCalendars() {
        val timeZone = PFDateTimeUtils.TIMEZONE_EUROPE_BERLIN
        val testDate = PFDateTime.withDate(2012, Month.DECEMBER, 23, 8, 33, 24, 123, timeZone.toZoneId());
        val utcDate: PFDateTime = PFDateTimeUtils.getUTCBeginOfDay(testDate.utilDate, timeZone)
        Assertions.assertEquals("2012-12-23 00:00:00.000", utcDate.isoStringMilli)
        Assertions.assertEquals("2012-12-22 23:00:00.000", from(testDate.utilDate)!!.beginOfDay.isoStringMilli)
    }

    @Test
    fun parseUTCDateTest() {
        Assertions.assertEquals("2020-01-25 22:12:00.000", parseDate("2020-01-25T22:12:00.000Z"))
        Assertions.assertEquals("2020-01-25 22:12:00.000", parseDate("2020-01-25T23:12:00.000+01:00"))
        Assertions.assertEquals("2020-01-25 22:12:00.000", parseDate("2020-01-25T21:12:00.000-01:00"))
        Assertions.assertEquals("2020-01-25 22:12:00.000", parseDate("2020-01-25T23:12:00+01:00"))
        Assertions.assertEquals("2020-01-25 22:12:00.000", parseDate("2020-01-25 22:12:00"))
        Assertions.assertEquals("2020-01-25 22:12:00.000", parseDate("2020-01-25 23:12:00+01:00"))
        Assertions.assertEquals("2020-01-25 22:12:00.000", parseDate("2020-01-25 22:12"))
        Assertions.assertEquals("2020-01-25 22:12:00.000", parseDate("1579990320"))
        Assertions.assertEquals("2020-01-25 22:12:00.000", parseDate("1579990320000", PFDateTime.NumberFormat.EPOCH_MILLIS))
    }

    @Test
    fun parseDateTest() {
        // Timezone is ignored, because time zone is given in string to parse:
        parseAndCheck("2020-01-25 22:12:00.000", PFDateTimeUtils.ZONE_EUROPE_BERLIN, "2020-01-25T22:12:00.000Z", PFDateTimeUtils.ZONE_EUROPE_BERLIN)
        parseAndCheck("2020-01-25 22:12:00.000", PFDateTimeUtils.ZONE_EUROPE_BERLIN,"2020-01-25T23:12:00.000+01:00", PFDateTimeUtils.ZONE_EUROPE_BERLIN)
        parseAndCheck("2020-01-25 22:12:00.000", PFDateTimeUtils.ZONE_EUROPE_BERLIN,"2020-01-25T21:12:00.000-01:00", PFDateTimeUtils.ZONE_EUROPE_BERLIN)
        parseAndCheck("2020-01-25 22:12:00.000", PFDateTimeUtils.ZONE_EUROPE_BERLIN,"2020-01-25T23:12:00+01:00", PFDateTimeUtils.ZONE_EUROPE_BERLIN)
        parseAndCheck("2020-01-25 22:12:00.000", PFDateTimeUtils.ZONE_EUROPE_BERLIN,"2020-01-25 23:12:00+01:00", PFDateTimeUtils.ZONE_EUROPE_BERLIN)

        // Timezone should be used:
        parseAndCheck("2020-01-25 21:12:00.000",PFDateTimeUtils.ZONE_EUROPE_BERLIN,"2020-01-25 22:12:00", PFDateTimeUtils.ZONE_EUROPE_BERLIN)
        parseAndCheck("2020-01-25 21:12:00.000", PFDateTimeUtils.ZONE_EUROPE_BERLIN,"2020-01-25 22:12", PFDateTimeUtils.ZONE_EUROPE_BERLIN)
        parseAndCheck("2020-01-25 22:12:00.000", PFDateTimeUtils.ZONE_EUROPE_BERLIN,"1579990320", PFDateTimeUtils.ZONE_EUROPE_BERLIN)
        parseAndCheck("2020-01-25 22:12:00.000", PFDateTimeUtils.ZONE_EUROPE_BERLIN,"1579990320000", PFDateTimeUtils.ZONE_EUROPE_BERLIN, PFDateTime.NumberFormat.EPOCH_MILLIS)
    }

    private fun parseDate(text: String?, numberFormat: PFDateTime.NumberFormat = PFDateTime.NumberFormat.EPOCH_SECONDS): String? {
        return PFDateTimeUtils.parseAndCreateDateTime(text, PFDateTimeUtils.ZONE_UTC, numberFormat = numberFormat)!!.isoStringMilli
    }

    private fun parseAndCheck(expectedDate: String, expectedZoneId: ZoneId, text: String, zoneId: ZoneId, numberFormat: PFDateTime.NumberFormat = PFDateTime.NumberFormat.EPOCH_SECONDS) {
        val dateTime = PFDateTimeUtils.parseAndCreateDateTime(text, zoneId, numberFormat = numberFormat)
        Assertions.assertEquals(expectedDate, dateTime!!.isoStringMilli)
        Assertions.assertEquals(expectedZoneId, dateTime.zone)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestSetup.init()
        }
    }
}
