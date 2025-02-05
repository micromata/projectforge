/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

class TextFieldModificationTest {
  class A(var text: String?)

  @Test
  fun getNewTextValueTest() {
    checkNewTextValue("", "", "", "empty string to delete", delete = true)
    checkNewTextValue("test", "", "", "delete string", delete = true)
    checkNewTextValue("test", "", "test", "Can't delete and append text!", delete = true, append = true)
    checkNewTextValue("test", "new text", "test\nnew text", "new value should be appended", append = true)
    checkNewTextValue(
      "test\nnew text",
      "new text",
      null,
      "new value is already part of old value, no modification expected.",
      append = true,
    )
    checkNewTextValue("The lazy fox jumps.", "lazy", "The fox jumps.", "delete string", delete = true)
    checkNewTextValue("The\n lazy fox jumps.", "Lazy", "The fox jumps.", "delete string", delete = true)
    checkNewTextValue(
      "The\n lazyfox jumps.",
      "Lazy",
      "Thefox jumps.",
      "delete string but preserve any white spaces",
      delete = true,
    )
    checkNewTextValue(
      "The\n lazy \nfox jumps.",
      "Lazy",
      "The\nfox jumps.",
      "delete string but preserve any white spaces",
      delete = true,
    )
    checkNewTextValue(
      "This is a text.\n This should be deleted.\nFollowing text.",
      "This should be deleted.",
      "This is a text.\nFollowing text.",
      "delete string",
      delete = true,
    )

    checkNewTextValue("The lazy fox jumps.\n", "append text", "The lazy fox jumps.\nappend text", "trim end and append string", append = true)


    checkNewTextValue("The lazy fox jumps.\nHurz", "hUrz", "The lazy fox jumps.", "delete string", delete = true)
    checkNewTextValue("The lazy fox jumps.\n  Hurz", "hUrz", "The lazy fox jumps.", "delete string", delete = true)

    checkNewTextValue("The lazy fox jumps.", "lazy", "The old fox jumps.", "delete string", replaceText = "old")
    checkNewTextValue("The lazy fox jumps.", "Lazy", "The old fox jumps.", "delete string", replaceText = "old")

    checkNewTextValue("test", "lazy", "test", "Can't handle multiple actions.", delete = true, replaceText = "old")
    checkNewTextValue("test", "lazy", "test", "Can't handle multiple actions.", append = true, replaceText = "old")
  }

  private fun checkNewTextValue(
    oldValue: String?,
    newValue: String?,
    expectedResult: String?,
    message: String,
    delete: Boolean? = null,
    append: Boolean? = null,
    replaceText: String? = null,
  ) {
    val param = MassUpdateParameter("param", "displayName")
    param.textValue = newValue
    param.delete = delete
    param.append = append
    param.replaceText = replaceText
    val result = TextFieldModification.getNewTextValue(oldValue, param)
    val data = A(oldValue)
    TextFieldModification.processTextParameter(data, "text", mapOf("text" to param))
    if (expectedResult == null) {
      Assertions.assertNull(result, message)
      Assertions.assertEquals(oldValue, data.text, message) // Field unmodified.
    } else {
      Assertions.assertEquals(expectedResult, result, message)
      Assertions.assertEquals(expectedResult, data.text, message)
    }
  }
}
