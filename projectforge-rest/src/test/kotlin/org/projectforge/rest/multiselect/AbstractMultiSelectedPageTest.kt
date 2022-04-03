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

class AbstractMultiSelectedPageTest {
  class A(var text: String?)

  @Test
  fun getNewTextValueTest() {
    checkNewTextValue("", true, "", false, "", "empty string to delete")
    checkNewTextValue("", true, "", true, "", "empty string to delete (append param without effect)")
    checkNewTextValue("test", true, "", false, "", "delete string")
    checkNewTextValue("test", true, "", true, "test", "Can't delete and append text!")
    checkNewTextValue("test", false, "new text", true, "test\nnew text", "new value shoud be appended")
    checkNewTextValue(
      "test\nnew text",
      false,
      "new text",
      true,
      null,
      "new value is already part of old value, no modification expected."
    )
    checkNewTextValue("The lazy fox jumps.", true, "lazy", false, "The fox jumps.", "delete string")
    checkNewTextValue("The\n lazy fox jumps.", true, "Lazy", false, "The fox jumps.", "delete string")
    checkNewTextValue("The\n lazyfox jumps.", true, "Lazy", false, "Thefox jumps.", "delete string but preserve any white spaces")
    checkNewTextValue("The\n lazy \nfox jumps.", true, "Lazy", false, "The\nfox jumps.", "delete string but preserve any white spaces")
    checkNewTextValue(
      "This is a text.\n This should be deleted.\nFollowing text.",
      true,
      "This should be deleted.",
      false,
      "This is a text.\nFollowing text.",
      "delete string"
    )
  }

  private fun checkNewTextValue(
    oldValue: String?,
    delete: Boolean?,
    newValue: String?,
    append: Boolean,
    expectedResult: String?,
    message: String,
  ) {
    val param = MassUpdateParameter()
    param.delete = delete
    param.textValue = newValue
    param.append = append
    val result = AbstractMultiSelectedPage.getNewTextValue(oldValue, param)
    val data = A(oldValue)
    AbstractMultiSelectedPage.processTextParameter(data, "text", mapOf("text" to param))
    if (expectedResult == null) {
      Assertions.assertNull(result, message)
      Assertions.assertEquals(oldValue, data.text, message) // Field unmodified.
    } else {
      Assertions.assertEquals(expectedResult, result, message)
      Assertions.assertEquals(expectedResult, data.text, message)
    }
  }
}
