/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.merlin

import com.fasterxml.jackson.annotation.JsonProperty
import de.micromata.merlin.word.templating.DependentVariableDefinition
import de.micromata.merlin.word.templating.VariableDefinition
import de.micromata.merlin.word.templating.VariableType
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.ui.UIColor
import java.math.BigDecimal

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MerlinVariable(
  val id: Int,
  val name: String,
  definition: VariableDefinition? = null,
  dependentVariableDefinition: DependentVariableDefinition? = null,
  var used: Boolean? = null,
  var masterVariable: Boolean? = null,
) {
  var description: String? = null
  var required: Boolean = false
  var unique: Boolean = false
  var minimumValue: Any? = null
  var maximumValue: Any? = null
  var allowedValues: List<Any>? = null
  var type: VariableType = VariableType.STRING
  var defined: Boolean = false
  var dependsOn: String? = null
  var mapping: Map<Any, Any>? = null

  @get:JsonProperty
  val allowedValuesFormatted: String?
    get() = allowedValues?.joinToString { "$it" }

  @get:JsonProperty
  val mappingFormatted: String?
    get() = mapping?.entries?.joinToString { "${it.key}=>${it.value}" }

  val dependent: Boolean
    get() = dependsOn != null

  init {
    if (definition != null) {
      defined = true
      description = definition.description
      required = definition.isRequired
      unique = definition.isUnique
      minimumValue = definition.minimumValue
      maximumValue = definition.maximumValue
      allowedValues = definition.allowedValuesList
      type = definition.type
    }
    if (dependentVariableDefinition != null) {
      mapping = dependentVariableDefinition.mapping
      dependsOn = dependentVariableDefinition.dependsOn.name ?: "???"
    }
  }

  val input: Boolean
    get() = defined && !dependent

  val uiColor: UIColor?
    get() {
      return if (masterVariable == true) {
        UIColor.DANGER
      } else if (input) {
        UIColor.SUCCESS
      } else if (used == false) {
        UIColor.LIGHT
      } else if (dependent) {
        UIColor.SECONDARY
      } else {
        null
      }
    }

  fun validate(value: Any?): String? {
    if (!defined) {
      return null
    }
    val empty = value == null || value is String && value.isEmpty()
    if (required) {
      if (empty) {
        return translateMsg("validation.error.fieldRequired", name)
      }
    }
    if (!empty) {
      val list = allowedValues
      if (!list.isNullOrEmpty()) {
        var matches = false
        list.forEach {
          if (it == value) {
            matches = true
          }
        }
        if (!matches) {
          return translateMsg(
            "plugins.merlin.validation.valueDoesNotMatchOptions",
            name,
            value,
            list.joinToString { "$it" })
        }
      }
      val bdValue = asBigDecimal(value)
      if (bdValue != null) {
        val minimumBD = asBigDecimal(minimumValue)
        if (minimumBD != null) {
          if (type.isIn(VariableType.INT, VariableType.FLOAT, VariableType.STRING)) {
            if (minimumBD > bdValue) {
              return translateMsg(
                "plugins.merlin.validation.valueToLow",
                name,
                value,
                minimumValue
              )
            }
          }
        }
        val maximumBD = asBigDecimal(maximumValue)
        if (maximumBD != null) {
          if (type.isIn(VariableType.INT, VariableType.FLOAT, VariableType.STRING)) {
            if (maximumBD < bdValue) {
              return translateMsg(
                "plugins.merlin.validation.valueToHigh",
                name,
                value,
                maximumValue
              )
            }
          }
        }
      }
    }
    return null
  }

  fun asBigDecimal(value: Any?): BigDecimal? {
    value ?: return null
    if (value is Number) {
      return BigDecimal(value.toString())
    }
    if (value is String) {
      return NumberHelper.parseBigDecimal(value)
    }
    return null
  }
}
