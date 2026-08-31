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

package org.projectforge.business.calendar

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CalendarStyleTest {
    @Test
    fun hexToRGBTest() {
        checkRGB(0, 17, 34, CalendarStyle.hexToRGB("#012"))
        checkRGB(0, 17, 34, CalendarStyle.hexToRGB("#001122"))
        checkRGB(163, 39, 255, CalendarStyle.hexToRGB("#a327ff"))
    }

    @Test
    fun validateHexCodeTest() {
        assertTrue(CalendarStyle.validateHexCode("#123"))
        assertTrue(CalendarStyle.validateHexCode("#0ff"))
        assertTrue(CalendarStyle.validateHexCode("#123456"))
        assertTrue(CalendarStyle.validateHexCode("#a789bc"))

        assertFalse(CalendarStyle.validateHexCode("123"))
        assertFalse(CalendarStyle.validateHexCode("abc123"))
        assertFalse(CalendarStyle.validateHexCode("#12"))
        assertFalse(CalendarStyle.validateHexCode("#1"))
        assertFalse(CalendarStyle.validateHexCode("#12345"))
        assertFalse(CalendarStyle.validateHexCode("#1234567"))
        assertFalse(CalendarStyle.validateHexCode("#gff"))
    }

    @Test
    fun darkModeTextColorTest() {
        // Transparent (STANDARD) scheme: light mode darkens the base colour, dark mode lightens it, so the
        // dark-mode text is brighter than the light-mode text for the same calendar colour.
        val base = "#2f65c8"
        val light = CalendarStyle.getTextColor(base, CalendarEventColorScheme.STANDARD, darkMode = false)
        val dark = CalendarStyle.getTextColor(base, CalendarEventColorScheme.STANDARD, darkMode = true)
        assertNotEquals(light, dark)
        assertTrue(brightness(dark) > brightness(light), "dark-mode text ($dark) should be brighter than light-mode text ($light)")

        // Classic scheme is theme-independent (solid backgrounds), so dark mode changes nothing.
        assertEquals(
            CalendarStyle.getTextColor(base, CalendarEventColorScheme.CLASSIC, darkMode = false),
            CalendarStyle.getTextColor(base, CalendarEventColorScheme.CLASSIC, darkMode = true),
        )
    }

    private fun brightness(hexColor: String): Int {
        val rgb = CalendarStyle.hexToRGB(hexColor)
        return (rgb.r * 299 + rgb.g * 587 + rgb.b * 114) / 1000
    }

    private fun checkRGB(r:Int, g:Int, b:Int, rgb: CalendarStyle.RGB) {
        assertEquals(r, rgb.r)
        assertEquals(g, rgb.g)
        assertEquals(b, rgb.b)
    }
}
