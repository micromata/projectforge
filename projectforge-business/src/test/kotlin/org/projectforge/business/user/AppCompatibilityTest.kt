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

package org.projectforge.business.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppCompatibilityTest {
    @Test
    fun `older apps are outdated, equal and newer ones not`() {
        assertTrue(AppCompatibility.isOutdated("0.9.9"))
        assertTrue(AppCompatibility.isOutdated("1.0.0+12"))
        assertFalse(AppCompatibility.isOutdated(AppCompatibility.MIN_APP_VERSION))
        assertFalse(AppCompatibility.isOutdated("${AppCompatibility.MIN_APP_VERSION}+1"))
        assertFalse(AppCompatibility.isOutdated("1.0.10"))
        assertFalse(AppCompatibility.isOutdated("2.0"))
    }

    @Test
    fun `an unknown or unparseable version is never outdated`() {
        assertFalse(AppCompatibility.isOutdated(null))
        assertFalse(AppCompatibility.isOutdated(""))
        assertFalse(AppCompatibility.isOutdated("garbage"))
        assertFalse(AppCompatibility.isOutdated("v0.1"))
    }

    @Test
    fun `versions are compared by numeric segments`() {
        assertEquals(listOf(1, 0, 1), AppCompatibility.parse("1.0.1+3"))
        assertEquals(listOf(9, 2, 4), AppCompatibility.parse("9.2.4-SNAPSHOT"))
        assertNull(AppCompatibility.parse("abc"))
        assertEquals(0, AppCompatibility.compare(listOf(1, 0), listOf(1, 0, 0)))
        assertTrue(AppCompatibility.compare(listOf(1, 0, 9), listOf(1, 0, 10)) < 0)
        assertTrue(AppCompatibility.compare(listOf(2), listOf(1, 99)) > 0)
    }
}
