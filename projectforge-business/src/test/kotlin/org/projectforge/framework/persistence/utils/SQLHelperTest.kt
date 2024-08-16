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

package org.projectforge.framework.persistence.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SQLHelperTest {
  @Test
  fun getYearsTest() {
    Assertions.assertArrayEquals(SQLHelper.getYears(2022, null), intArrayOf(2022))
    Assertions.assertArrayEquals(SQLHelper.getYears(null, 2022), intArrayOf(2022))
    Assertions.assertArrayEquals(SQLHelper.getYears(2022, 2022), intArrayOf(2022))
    Assertions.assertArrayEquals(SQLHelper.getYears(2022, 2023), intArrayOf(2022, 2023))
    Assertions.assertArrayEquals(SQLHelper.getYears(2022, 2025), intArrayOf(2022, 2023, 2024, 2025))
  }
}
