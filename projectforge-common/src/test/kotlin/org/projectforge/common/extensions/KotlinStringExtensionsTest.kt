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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class KotlinStringExtensionsTest {
    @Test
    fun `test abbreviate method`() {
        assertEquals("", null.abbreviate(10))
        assertEquals("", "".abbreviate(10))
        assertEquals("1234567890", "1234567890".abbreviate(10))
        assertEquals("1234567...", "1234567890a".abbreviate(10))
    }

    @Test
    fun `test capitalize method`() {
        assertEquals("", "".capitalize())
        assertEquals(" a", " a".capitalize())
        assertEquals("A", " a".capitalize(trimValues = true))
        assertEquals("A", "a".capitalize())
        assertEquals("A", "A".capitalize())
        assertEquals("Alpha", "alpha".capitalize())
        assertEquals("Alpha", "alpha".capitalize())
    }

    @Test
    fun `test isEqualsTo`() {
        assertTrue(null.isEqualsTo(null))
        assertTrue("".isEqualsTo(null))
        assertTrue("".isEqualsTo(""))
        assertFalse(" ".isEqualsTo(null))
        assertFalse(null.isEqualsTo("  "))

        assertTrue(null.isEqualsTo(null, trimValues = true))
        assertTrue(null.isEqualsTo("  ", trimValues = true))
        assertTrue(" ".isEqualsTo(null, trimValues = true))
        assertTrue(" hallo   ".isEqualsTo("hallo", trimValues = true))
    }
}
