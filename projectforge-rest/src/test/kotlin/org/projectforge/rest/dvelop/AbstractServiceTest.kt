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

package org.projectforge.rest.dvelop

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class AbstractServiceTest {
  @Test
  fun getPrioritizedTest() {
    checkPrioritizedString(null, null, null, false)
    checkPrioritizedString("", null, "", false)
    checkPrioritizedString(null, "", null, false)
    checkPrioritizedString("", "", "", false)
    checkPrioritizedString(null, " ", null, false)
    checkPrioritizedString(" ", null, " ", false)
    checkPrioritizedString(" ", "", " ", false)
    checkPrioritizedString("a", null, "a", true)
    checkPrioritizedString(" ", "b", "b", true)
    checkPrioritizedString(null, "b", "b", true)
    checkPrioritizedString("a", "b", "a", true)

    checkPrioritizedValue(null, null, null, false)
    checkPrioritizedValue(null, 42, 42, true)
    checkPrioritizedValue(42, null, 42, true)
    checkPrioritizedValue(42, 43, 42, true)
  }

  private fun checkPrioritizedString(p1: String?, p2: String?, exptectedResult: String?, expectedModified: Boolean) {
    val updateContext = AbstractDvelopService.UpdateContext()
    Assertions.assertEquals(exptectedResult,  AbstractDvelopService.getPrioritizedString(p1, p2, updateContext))
    Assertions.assertEquals(expectedModified, updateContext.modified)

    checkPrioritizedValue(p1, p2, exptectedResult, expectedModified)
  }

  private fun <T>checkPrioritizedValue(p1: T?, p2: T?, exptectedResult: T?, expectedModified: Boolean) {
    val updateContext = AbstractDvelopService.UpdateContext()
    Assertions.assertEquals(exptectedResult,  AbstractDvelopService.getPrioritizedValue(p1, p2, updateContext))
    Assertions.assertEquals(expectedModified, updateContext.modified)
  }
}
