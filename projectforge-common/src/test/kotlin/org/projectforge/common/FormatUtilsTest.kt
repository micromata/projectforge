/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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
import java.util.*

class FormatUtilsTest {
    @Test
    fun formatBytesTest() {
        val locale = Locale.ENGLISH
        Assertions.assertEquals("--", FormatterUtils.formatBytes(null as Long?, locale))
        var scale = 1L
        formatBytesTest(scale, "bytes", locale)
        scale *= 1024
        formatBytesTest(scale, "KB", locale)
        scale *= 1024
        formatBytesTest(scale, "MB", locale)
        scale *= 1024
        formatBytesTest(scale, "GB", locale)
        scale *= 1024
        formatBytesTest(scale, "TB", locale)
    }

    private fun formatBytesTest(scale: Long, unit: String, locale: Locale) {
        Assertions.assertEquals("1$unit", FormatterUtils.formatBytes(scale * 1L, locale))
        Assertions.assertEquals("1,023$unit", FormatterUtils.formatBytes(scale * 1023L, locale))
    }
}
