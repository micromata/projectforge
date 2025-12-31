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

package org.projectforge.common

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.common.AutoCompletionUtils

class AutoCompletionUtilsTest {
  @Test
  fun containsAllTest() {
    Assertions.assertTrue(AutoCompletionUtils.containsAll("Uni Kassel", listOf("Uni")))
    Assertions.assertTrue(AutoCompletionUtils.containsAll("Uni Kassel", listOf("Kassel", "uni")))
    Assertions.assertTrue(AutoCompletionUtils.containsAll("Uni Kassel", listOf("kassel")))
    Assertions.assertFalse(AutoCompletionUtils.containsAll("Uni Kassel", listOf("Kassel", "Somewhere")))
  }


  @Test
  fun filterTest() {
    val list = listOf("Uni Kassel", "Uni Göttingen", "test", "Kassel", "Uni somewhere")
    checkList(AutoCompletionUtils.filter(list, "Uni"), "Uni Göttingen", "Uni Kassel", "Uni somewhere")
    checkList(AutoCompletionUtils.filter(list, "Uni Kassel"), "Uni Kassel")
    checkList(AutoCompletionUtils.filter(list, "Uni s"), "Uni Kassel", "Uni somewhere")
    checkList(AutoCompletionUtils.filter(list, "Kassel"), "Kassel", "Uni Kassel")
    checkList(AutoCompletionUtils.filter(list, "hurzel"))
  }

  private fun checkList(list: List<String>, vararg expected: String) {
    Assertions.assertEquals(expected.size, list.size)
    list.forEachIndexed { i, it ->
      Assertions.assertEquals(it, expected[i])
    }
  }
}
