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

package org.projectforge.rest.multiselect

import org.projectforge.framework.DisplayNameCapable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

/**
 * Contains a field for mass update (string, int, number, date, task, user etc.) and the checkbox
 * either the field should be changed or not.
 */
class MassUpdateParameter: DisplayNameCapable {
  /**
   * If true, the value should be deleted (if delete option is used).
   */
  var delete: Boolean? = null

  /**
   * If true, the value should be changed by the given value (if change option is used).
   */
  var change: Boolean? = null

  /**
   * If true, the value of the text field should be appended.
   */
  var append: Boolean? = null

  /**
   * The given textValue should be replaced by this string (for text fields)
   */
  var replaceText: String? = null

  /**
   * Number of actions (delete, append and replace). More than 1 will normally result in an error and 0 means nothing to-do.
   */
  val actionCounter: Int
    get() {
      var actionCounter = 0
      if (delete == true) ++actionCounter
      if (!replaceText.isNullOrEmpty()) ++actionCounter // Replace text is given, covered by !isEmpty
      if (append == true && (actionCounter > 0 || !textValue.isNullOrBlank())) ++actionCounter // Given text should be appended
      if (!isEmpty()) {
        if (textValue.isNullOrBlank()) {
          actionCounter++
        } else {
          // Text modification
          if (actionCounter == 0) {
            ++actionCounter // Only if not combined with a previous action.
          }
        }
      }
      return actionCounter
    }

  val hasAction: Boolean
    get() = actionCounter == 1

  val error: String?
    get() {
      if (actionCounter > 1) {
        // Can't only proceed with one of the action (delete, or append or replace).
        return "massUpdate.error.invalidOptionMix"
      }
      if (!replaceText.isNullOrBlank() && textValue.isNullOrBlank()) {
        return "massUpdate.error.textValueToReplaceMissed"
      }
      return null
    }

  val hasError: Boolean
    get() = error != null

  /**
   * E. g. for tasks, the id of the selected task is set.
   */
  var id: Int? = null
  var textValue: String? = null
  var intValue: Int? = null
  var decimalValue: BigDecimal? = null
  var localDateValue: LocalDate? = null
  var timestampValue: Date? = null
  var timeValue: LocalTime? = null
  var booleanValue: Boolean? = null
  override var displayName: String? = null

  fun isEmpty(): Boolean {
    return textValue.isNullOrBlank() &&
        intValue == null &&
        decimalValue == null &&
        localDateValue == null &&
        timestampValue == null &&
        timeValue == null &&
        booleanValue == null
  }
}
