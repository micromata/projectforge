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

package org.projectforge.plugins.cveradar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DpkgVersionComparatorTest {
    private fun cmp(a: String, b: String) = DpkgVersionComparator.compare(a, b).coerceIn(-1, 1)

    @Test
    fun `versions of Debian and Ubuntu compare as dpkg does`() {
        assertEquals(0, cmp("7.81.0-1ubuntu1.30", "7.81.0-1ubuntu1.30"))
        assertEquals(1, cmp("7.81.0-1ubuntu1.30", "7.81.0-1ubuntu1.3"), "30 > 3")
        assertEquals(-1, cmp("2.9.14+dfsg-1.3~deb12u1", "2.9.14+dfsg-1.3"), "~ before the end")
        assertEquals(-1, cmp("1.0~rc1", "1.0"))
        assertEquals(-1, cmp("1.0~rc1", "1.0~rc2"))
        assertEquals(1, cmp("1.0a", "1.0"), "a letter after the end")
        assertEquals(1, cmp("1.0+b1", "1.0"), "a binary rebuild")
        assertEquals(-1, cmp("1.0a", "1.0+"), "letters before other characters")
        assertEquals(1, cmp("1:1.0-1", "2.0-1"), "the epoch first")
        assertEquals(0, cmp("0:1.0-1", "1.0-1"), "epoch 0 as none")
        assertEquals(-1, cmp("3.0.2-0ubuntu1.10", "3.0.2-0ubuntu1.18"))
        assertEquals(-1, cmp("3.0.15-1~deb12u1", "3.0.19-1~deb12u1"))
        assertEquals(1, cmp("1.35+dfsg-3.1", "1.35+dfsg-3"), "the revision")
        assertEquals(1, cmp("1.2-10", "1.2-9"))
        assertEquals(0, cmp("1.002", "1.2"), "leading zeros")
        assertEquals(1, cmp("1.99999999999999999999", "1.9999999999999999999"), "no overflow")
        assertTrue(DpkgVersionComparator.compare("7.84.0-1", "0") > 0)
    }
}
