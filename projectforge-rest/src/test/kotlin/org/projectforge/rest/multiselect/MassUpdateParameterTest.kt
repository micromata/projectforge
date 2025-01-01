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
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class MassUpdateParameterTest {
  @Test
  fun hasActionTest() {
    check(false, msg = "empty parameter has no action")
    check(true, textValue = "new text", msg = "parameter has action to change text")
    check(true, textValue = "new text", replaceText = "replace", msg = "parameter has action to replace text")
    check(
      false,
      error = "massUpdate.error.invalidOptionMix",
      append = true,
      textValue = "new text",
      replaceText = "replace",
      msg = "parameter can't has action to replace text AND append"
    )
    check(
      false,
      error = "massUpdate.error.invalidOptionMix",
      append = true,
      delete = true,
      textValue = "new text",
      replaceText = "replace",
      msg = "parameter can't has action to replace text AND append AND delete"
    )
    check(
      false,
      error = "massUpdate.error.invalidOptionMix",
      append = true,
      delete = true,
      textValue = "new text",
      msg = "parameter can't has action to delete AND append"
    )

    check(true, append = true, textValue = "new text", msg = "parameter has append action")
    check(false, append = true, msg = "parameter has no action (append without text)")

    check(true, delete = true, msg = "parameter has action to delete field")
    check(true, delete = true, textValue = "part to delete", msg = "parameter has action to delete occurrences of text")
    check(false, change = true, msg = "change of id not handled by MassUpdateParameter")
    check(false, change = true, id = 42, msg = "change of id not handled by MassUpdateParameter")

    check(true, localeDateValue = LocalDate.now(), msg = "date should be changed")
    check(
      false,
      error = "massUpdate.error.invalidOptionMix",
      delete = true,
      localeDateValue = LocalDate.now(),
      msg = "Can't delete date, it's given."
    )
  }

  private fun check(
    hasAction: Boolean,
    textValue: String? = null,
    localeDateValue: LocalDate? = null,
    booleanValue: Boolean? = null,
    decimalValue: BigDecimal? = null,
    intValue: Int? = null,
    timeValue: LocalTime? = null,
    timestampValue: Date? = null,
    replaceText: String? = null,
    delete: Boolean? = null,
    append: Boolean? = null,
    change: Boolean? = null,
    id: Long? = null,
    error: String? = null,
    msg: String? = null,
  ) {
    val param = MassUpdateParameter()
    param.textValue = textValue
    param.localDateValue = localeDateValue
    param.replaceText = replaceText
    param.delete = delete
    param.append = append
    param.change = change
    param.booleanValue = booleanValue
    param.decimalValue = decimalValue
    param.longValue = intValue
    param.timeValue = timeValue
    param.timestampValue = timestampValue
    param.id = id
    val params = listOf(
      Param("append", append),
      Param("delete", delete),
      Param("change", change),
      Param("textValue", textValue),
      Param("localDateValue", localeDateValue),
      Param("booleanValue", booleanValue),
      Param("decimalValue", decimalValue),
      Param("intValue", intValue),
      Param("timeValue", timeValue),
      Param("timestampValue", timestampValue),
      Param("replaceText", replaceText),
      Param("id", id),
    )
    val message = "$msg: ${params.filter { it.value != null }.joinToString { "${it.name}=${it.value}" }} "
    Assertions.assertEquals(hasAction, param.hasAction, message)
    Assertions.assertEquals(error, param.error, message)
  }

  private class Param(val name: String, val value: Any?)
}
