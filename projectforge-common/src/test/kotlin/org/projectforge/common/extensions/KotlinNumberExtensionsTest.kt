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

package org.projectforge.common.extensions

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

class KotlinNumberExtensionsTest {
    @Test
    fun `test formatting of numbers`() {
        Locale.setDefault(Locale.ENGLISH)
        Assertions.assertEquals("", null.format())
        Assertions.assertEquals("1,234.57", 1234.5678.format(scale = 2))
        Assertions.assertEquals("1,234.00", 1234.format(scale = 2))
        Assertions.assertEquals("1,234", 1234.format())
    }

    @Test
    fun `test formatting of millis`() {
        Assertions.assertEquals("", null.formatMillis())
        Assertions.assertEquals("00:00.123", 123.formatMillis())
        Assertions.assertEquals("00:01.123", (1123).formatMillis())
        Assertions.assertEquals("01:01.123", (61123).formatMillis())
        Assertions.assertEquals("12:21.123", (12 * 60000 + 21 * 1000 + 123).formatMillis())
    }

    @Test
    fun `test format digits`() {
        Assertions.assertEquals("??", null.format2Digits())
        Assertions.assertEquals("00", 0.format2Digits())
        Assertions.assertEquals("01", 1.format2Digits())
        Assertions.assertEquals("123", 123.format2Digits())
        Assertions.assertEquals("-1", (-1).format2Digits())
        Assertions.assertEquals("-12", (-12).format2Digits())
        Assertions.assertEquals("1.24", (1.24).format2Digits())

        Assertions.assertEquals("???", null.format3Digits())
        Assertions.assertEquals("000", 0.format3Digits())
        Assertions.assertEquals("001", 1.format3Digits())
        Assertions.assertEquals("1234", 1234.format3Digits())
        Assertions.assertEquals("-01", (-1).format3Digits())
        Assertions.assertEquals("-12", (-12).format3Digits())
        Assertions.assertEquals("1.24", (1.24).format3Digits())
    }

    @Test
    fun `test formatting of number of bytes`() {
        Assertions.assertEquals("--", null.formatBytes())
        Assertions.assertEquals("0", 0.formatBytes())
        var scale = 1L
        formatBytesTest(scale, "bytes")
        scale *= 1024
        formatBytesTest(scale, "KB")
        scale *= 1024
        formatBytesTest(scale, "MB")
        scale *= 1024
        formatBytesTest(scale, "GB")
        scale *= 1024
        formatBytesTest(scale, "TB")
    }

    private fun formatBytesTest(scale: Long, unit: String) {
        Locale.setDefault(Locale.ENGLISH)
        Assertions.assertEquals("1$unit", scale.formatBytes())
        Assertions.assertEquals("1,023$unit", (scale * 1023).formatBytes())
    }
}
