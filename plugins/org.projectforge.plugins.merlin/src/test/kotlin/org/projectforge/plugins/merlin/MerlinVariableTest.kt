/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.merlin

import org.junit.Test
import org.junit.jupiter.api.Assertions

class MerlinVariableTest {

  @Test
  fun csvTest() {
    val variable = MerlinVariable()
    variable.mappingValues = null
    Assertions.assertEquals(0, variable.mappingValuesArray.size)
    variable.mappingValues = ""
    Assertions.assertEquals(0, variable.mappingValuesArray.size)
    variable.mappingValues = "one"
    Assertions.assertArrayEquals(arrayOf("one"), variable.mappingValuesArray)
    variable.mappingValues = "\"one a, one b\""
    Assertions.assertArrayEquals(arrayOf("one a, one b"), variable.mappingValuesArray)
    variable.mappingValues = "one, two"
    Assertions.assertArrayEquals(arrayOf("one", "two"), variable.mappingValuesArray)
    variable.mappingValues = "\"one a, one b\", two"
    Assertions.assertArrayEquals(arrayOf("one a, one b", "two"), variable.mappingValuesArray)

    variable.allowedValues = null
    Assertions.assertEquals("", variable.allowedValuesFormatted)
    variable.allowedValues = listOf()
    Assertions.assertEquals("", variable.allowedValuesFormatted)
    variable.allowedValues = listOf("one")
    Assertions.assertEquals("one", variable.allowedValuesFormatted)
    variable.allowedValues = listOf("one a, one b")
    Assertions.assertEquals("\"one a, one b\"", variable.allowedValuesFormatted)
    variable.allowedValues = listOf("one a, one b", "two")
    Assertions.assertEquals("\"one a, one b\", two", variable.allowedValuesFormatted)
    variable.allowedValues = listOf("one a, one b", "", "three")
    Assertions.assertEquals("\"one a, one b\", , three", variable.allowedValuesFormatted)
  }
}
