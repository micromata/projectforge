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

package org.projectforge.common

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.locale
import org.projectforge.framework.utils.NumberHelper.extractPhonenumber
import java.util.Locale

class KotlinNumberUtilsTest {
    @Test
    fun `test formatting of numbers`() {
        Locale.setDefault(Locale.ENGLISH)
        ThreadLocalUserContext.locale = Locale.GERMAN
        Assertions.assertEquals("", null.format())
        Assertions.assertEquals("1,234.57", 1234.5678.format(scale = 2))
        Assertions.assertEquals("1,234.00", 1234.format(scale = 2))
        Assertions.assertEquals("1,234", 1234.format())


        //Assertions.assertEquals("1.234,57", 1234.5678.format(type = NumberFormatType.CURRENCY))
    }

    @Test
    fun `test formatting of numbers for users`() {
        Locale.setDefault(Locale.ENGLISH)
        locale = Locale.GERMAN
        Assertions.assertEquals("", null.formatForUser())
        Assertions.assertEquals("1.234,57", 1234.5678.formatForUser(scale = 2))
        Assertions.assertEquals("1.234,00", 1234.formatForUser(scale = 2))
        Assertions.assertEquals("1.234", 1234.formatForUser())

        Locale.setDefault(Locale.GERMAN)
        locale = null
        Assertions.assertEquals("", null.formatForUser())
        Assertions.assertEquals("1.234,57", 1234.5678.formatForUser(scale = 2))
        Assertions.assertEquals("1.234,00", 1234.formatForUser(scale = 2))
        Assertions.assertEquals("1.234", 1234.formatForUser())
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
        locale = Locale.GERMAN
        Assertions.assertEquals("1$unit", scale.formatBytes())
        Assertions.assertEquals("1,023$unit", (scale*1023).formatBytes())
        Assertions.assertEquals("1$unit", scale.formatBytesForUser())
        Assertions.assertEquals("1.023$unit", (scale*1023).formatBytesForUser())
    }

}
