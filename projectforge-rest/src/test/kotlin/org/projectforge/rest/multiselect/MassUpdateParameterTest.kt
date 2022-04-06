/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.multiselect

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MassUpdateParameterTest {
  @Test
  fun hasActionTest() {
    val param = MassUpdateParameter()
    check(param, false, null)
    param.textValue = "new text"
    check(param, true, null)
    param.replaceText = "replace"
    check(param, true, null)
    param.append = true
    check(param, false, "massUpdate.error.invalidOptionMix")
    param.append = null

    param.delete = true
    check(param, false, "massUpdate.error.invalidOptionMix")
    param.delete = null
    param.replaceText = null

    param.append = true
    check(param, true, null)
    param.textValue = ""
    check(param, false, null)

    param.delete = true
    param.append = null
    check(param, true, null)
    param.textValue = "text to delete"
    check(param, true, null)
    param.textValue = null
    param.delete = null
    param.change = true
    check(param, false, null)
    param.id = 1234
    check(param, false, null, "id changing not handled here.")

    param.change = null
    param.id = null
    param.localDateValue = LocalDate.now()
    check(param, true, null)
  }

  private fun check(param: MassUpdateParameter, hasAction: Boolean, expectedError: String?, msg: String? = null) {
    Assertions.assertEquals(hasAction, param.hasAction, msg)
    Assertions.assertEquals(expectedError, TextFieldModification.hasError(param), msg)
  }
}
