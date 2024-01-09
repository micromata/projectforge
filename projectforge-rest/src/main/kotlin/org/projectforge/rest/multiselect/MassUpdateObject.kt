/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

/**
 * Stores the old and the new state of an object while mass update.
 */
abstract class MassUpdateObject<T>(
  oldStateObj: T,
  val massUpdateData: Map<String, MassUpdateParameter>,
  // Synthetic fields such as taskAndKost2 for time sheets should be ignored in old/new-value modification check.
  val ignoreFieldsForModificationCheck: List<String>?,
) {
  class FieldModification(val oldValue: Any?, val parameter: MassUpdateParameter?) {
    var newValue: Any? = null
  }

  abstract fun getId(): Int

  // key is field name.
  val fieldModifications = mutableMapOf<String, FieldModification>()

  /**
   * Identifier for user to identifier object.
   */
  lateinit var identifier: String

  var modifiedObj: T? = null

  init {
    massUpdateData.forEach { (field, param) ->
      if (ignoreFieldsForModificationCheck?.contains(field) != true) {
        fieldModifications[field] =
          FieldModification(BeanHelper.getNestedProperty(oldStateObj, field), massUpdateData[field])
      }
    }
  }

  fun hasModifications(): Boolean {
    fieldModifications.values.forEach {
      if (it.oldValue != it.newValue) {
        return true
      }
    }
    return false
  }

  fun setModifiedObject(modifiedObj: T, identifier: String) {
    this.identifier = identifier
    this.modifiedObj = modifiedObj
    massUpdateData.forEach { (field, _) ->
      if (ignoreFieldsForModificationCheck?.contains(field) != true) {
        val fieldModification =
          fieldModifications[field] ?: FieldModification(null, massUpdateData[field]) // Should already given.
        fieldModifications[field] = fieldModification
        fieldModification.newValue = BeanHelper.getNestedProperty(modifiedObj, field)
      }
    }
  }
}
