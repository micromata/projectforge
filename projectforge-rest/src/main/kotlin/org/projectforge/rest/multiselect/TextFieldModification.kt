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

import org.projectforge.common.BeanHelper

object TextFieldModification {
  fun processTextParameter(
    data: Any,
    property: String,
    params: Map<String, MassUpdateParameter>,
  ) {
    val param = params[property] ?: return
    if (param.delete == true && param.append == true) {
      // Can't append AND delete text.
      return
    }
    val oldValue = BeanHelper.getProperty(data, property) as String?
    getNewTextValue(oldValue, param)?.let { newValue ->
      BeanHelper.setProperty(data, property, newValue)
    }
  }

  /**
   * @param param If append value is true, the given textValue of param will be appended to the oldValue (if textValue isn't already
   * contained in oldValue, otherwise null is returned).
   * @return The new Value to set or null, if no modification should done..
   */
  fun getNewTextValue(oldValue: String?, param: MassUpdateParameter?): String? {
    param ?: return null
    var actionCounter = 0
    val replaceText = param.replaceText
    if (param.delete == true) ++actionCounter
    if (param.append == true) ++actionCounter
    if (!replaceText.isNullOrEmpty()) ++actionCounter
    if (actionCounter > 1) {
      // Can't only proceed with one of the action (delete, or append or replace).
      return oldValue
    }
    if (param.delete == true) {
      return if (param.textValue.isNullOrBlank()) {
        // Whole field should be empty:
        ""
      } else {
        // Only occurence of textValue should be deleted:
        // Delete full line:
        oldValue?.replace("""\n\s*${param.textValue}\s*\n""".toRegex(RegexOption.IGNORE_CASE), "\n")
          // Delete occurrence including leading spaces:
          ?.replace("""\s*${param.textValue}""".toRegex(RegexOption.IGNORE_CASE), "")
      }
    }
    param.textValue.let { newValue ->
      if (newValue.isNullOrBlank()) {
        return null // Nothing to do.
      }
      if (!replaceText.isNullOrEmpty()) {
        // Replace text
        return if (oldValue.isNullOrEmpty()) {
          oldValue
        } else {
          oldValue.replace(newValue, replaceText, ignoreCase = true)
        }
      }
      if (param.append != true || oldValue.isNullOrBlank()) {
        return param.textValue // replace oldValue by this value.
      }
      return if (!oldValue.contains(newValue.trim(), true)) {
        "$oldValue\n$newValue" // Append new value.
      } else {
        null // Leave it untouched, because the new value is already contained in old value.
      }
    }
  }
}
