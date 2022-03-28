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

import org.projectforge.rest.dto.Task
import org.projectforge.rest.dto.User
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

/**
 * Contains a field for mass update (string, int, number, date, task, user etc.) and the checkbox
 * either the field should be changed or not.
 */
class MassUpdateParameter {
  /**
   * If true, the value should be deleted.
   */
  var delete: Boolean? = null
  var textValue: String? = null
  var intValue: Int? = null
  var decimalValue: BigDecimal? = null
  var localDateValue: LocalDate? = null
  var timestampValue: Date? = null
  var timeValue: LocalTime? = null
  var booleanValue: Boolean? = null
  var taskValue: Task? = null
  var userValue: User? = null

  fun isEmpty(): Boolean {
    return textValue.isNullOrBlank() &&
        intValue == null &&
        decimalValue == null &&
        localDateValue == null &&
        timestampValue == null &&
        timeValue == null &&
        booleanValue == null &&
        taskValue == null &&
        userValue == null
  }
}
