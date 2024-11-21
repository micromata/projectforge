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

package org.projectforge.framework.time

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.business.test.TestSetup
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class DateParserTest {
    @Test
    fun `test parsing by autodetecting all known date formats`() {
        assert("2021-12-31T23:59:59Z", "2021-12-31T23:59:59Z", "ISO 8601 date-time with seconds")
        assert("2021-12-31T23:59:59.999Z", "2021-12-31T23:59:59.999Z", "ISO 8601 date-time with milliseconds")
        assert("2021-12-31T21:59:59Z", "2021-12-31T23:59:59+02:00", "ISO 8601 date-time with seconds and offset")
        assert("2022-01-01T01:59:59.999Z", "2021-12-31T23:59:59.999-02:00", "ISO 8601 date-time with milliseconds and offset")

        assert("2021-12-31T23:59:00Z", "2021-12-31T23:59Z", "ISO 8601 date-time without seconds")
        assert("2021-12-31T21:59:00Z", "2021-12-31T23:59+02:00", "ISO 8601 date-time without seconds and offset")
        assert("2022-01-01T01:59:00Z", "2021-12-31T23:59-02:00", "ISO 8601 date-time without seconds and offset")

        assert("2021-12-31T21:59:48Z", "20211231T235948+02:00", "ISO 8601 date-time without seconds and with reduced precision")
        assert("2022-01-01T01:59:48Z", "2021-12-31T23:59:48-02:00", "ISO 8601 date-time without seconds and with reduced precision")
        assert("2022-01-01T01:59:48Z", "20211231T235948-02:00", "ISO 8601 date-time without seconds and with reduced precision")

        assert("2021-12-31T23:59:00Z", "20211231T2359Z", "yyyyMMDDTHHmmZ")
        assert("2021-12-31T21:59:00Z", "20211231T2359+02:00", "yyyyMMDDTHHmm+HH:MM")
        assert("2022-01-01T01:59:00Z", "20211231T2359-02:00", "yyyyMMDDTHHmm-HH:MM")

        assert("2021-12-31", "2021-12-31", "ISO 8601 date")
        assert("2021-12-31", "20211231", "yyyyMMdd")

        assert("2021-12-31T23:59:59", "2021-12-31T23:59:59", "ISO 8601 date-time without seconds without time zone")
        assert("2021-12-31T23:59:59.999", "2021-12-31T23:59:59.999", "ISO 8601 date-time without seconds and with milliseconds without time zone")
        assert("2021-12-31T23:59:00", "2021-12-31T23:59", "ISO 8601 date-time without seconds without time zone")
        assert("2021-12-31T23:59:00", "20211231T2359", "yyyyMMDDTHHmm")

        assert("2024-11-16T22:36:12Z", "1731796572", "Epoch seconds")
        assert("2024-11-16T22:36:12.123Z", "1731796572123", "Epoch millis")
    }

    private fun assert(expected: String, input: String, msg: String? = null) {
        assertSub(expected, input, msg)
        if (input.contains('T')) {
            assertSub(expected, input.replace('T', ' '), msg)
        }
    }

    private fun assertSub(expected: String, input: String, msg: String? = null) {
        val date = DateParser.parse(input)
        Assertions.assertNotNull(date, msg)
        date!!
        val isoString = if (date is ZonedDateTime) {
            date.withZoneSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)
        } else if (date is LocalDateTime) {
            date.format(DateTimeFormatter.ISO_DATE_TIME)
        } else {
            date.toString()
        }
        Assertions.assertEquals(expected, isoString, msg)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestSetup.init()
        }
    }
}
