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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VersionComparatorTest {
    @Test
    fun `versions compare part by part, numerically`() {
        assertTrue(VersionComparator.isAtLeast("2.6.34", "2.6.34"))
        assertTrue(VersionComparator.isAtLeast("2.6.35", "2.6.34"))
        assertTrue(VersionComparator.isAtLeast("2.10.0", "2.9.9"), "10 > 9, not as text")
        assertTrue(VersionComparator.isAtLeast("v3.0", "2.99"))
        assertFalse(VersionComparator.isAtLeast("2.6.3", "2.6.34"))
        assertFalse(VersionComparator.isAtLeast("2.6", "2.6.1"), "a missing part is lower")
        assertTrue(VersionComparator.isAtLeast("2.6.0", "2.6.0-beta1"), "the release is higher than its beta")
        assertFalse(VersionComparator.isAtLeast(null, "1.0"))
        assertFalse(VersionComparator.isAtLeast("1.0", ""))
    }
}
