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
import de.micromata.merlin.csv.CSVStringUtils
import de.micromata.merlin.word.templating.DependentVariableDefinition
import de.micromata.merlin.word.templating.VariableDefinition
import de.micromata.merlin.word.templating.VariableType
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.ui.UIColor
import java.math.BigDecimal

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MerlinVariable {
  var id: Int? = null

  @PropertyInfo(i18nKey = "plugins.merlin.variable.name")
  var name: String = ""

  @PropertyInfo(i18nKey = "description")
  var description: String? = null

  @PropertyInfo(i18nKey = "plugins.merlin.variable.required")
  var required: Boolean = false

  @PropertyInfo(i18nKey = "plugins.merlin.variable.unique", additionalI18nKey = "plugins.merlin.variable.unique.info")
  var unique: Boolean = false

  @PropertyInfo(i18nKey = "plugins.merlin.variable.minValue")
  var minimumValue: Any? = null

  @PropertyInfo(i18nKey = "plugins.merlin.variable.maxValue")
  var maximumValue: Any? = null

  @PropertyInfo(
    i18nKey = "plugins.merlin.variable.allowedValues",
    additionalI18nKey = "plugins.merlin.variable.allowedValues.info"
  )
  var allowedValues: List<String>? = null

  @PropertyInfo(i18nKey = "plugins.merlin.variable.type")
  var type: VariableType = VariableType.STRING
  var defined: Boolean = false

  var dependsOn: MerlinVariable? = null
    set(value) {
      field = value
      dependsOnName = value?.name
    }

  @PropertyInfo(i18nKey = "plugins.merlin.variable.dependsOn")
  var dependsOnName: String? = null

  @PropertyInfo(i18nKey = "plugins.merlin.variable.mapping", additionalI18nKey = "plugins.merlin.variable.mapping.info")
  var mappingValues: String? = null

  @get:JsonProperty
  val mappingMasterValues
    get() = dependsOn?.allowedValues?.joinToString { it }

  @get:JsonProperty
  val allowedValuesFormatted: String?
    get() = allowedValues?.joinToString { it }

  @PropertyInfo(i18nKey = "plugins.merlin.variable.used", additionalI18nKey = "plugins.merlin.variable.used.info")
  var used: Boolean? = null

  @PropertyInfo(i18nKey = "plugins.merlin.variable.master", additionalI18nKey = "plugins.merlin.variable.master.info")
  var masterVariable: Boolean? = null

  val dependent: Boolean
    get() = dependsOn != null

  val input: Boolean
    get() = defined && !dependent

  val uiColor: UIColor?
    get() {
      return when {
        masterVariable == true -> {
          UIColor.DANGER
        }
        input -> {
          UIColor.SUCCESS
        }
        used == false -> {
          UIColor.LIGHT
        }
        dependent -> {
          UIColor.SECONDARY
        }
        else -> {
          null
        }
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
            list.joinToString { it })
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

  /**
   * Only non-null values will be set.
   * @return this for chaining.
   */
  fun with(
    id: Int? = null,
    name: String? = null,
    used: Boolean? = null,
    masterVariable: Boolean? = null
  ): MerlinVariable {
    id?.let { this.id = id }
    name?.let { this.name = name }
    used?.let { this.used = used }
    masterVariable?.let { this.masterVariable = masterVariable }
    return this
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

  fun copyFrom(src: MerlinVariable) {
    this.type = src.type
    this.dependsOn = src.dependsOn
    this.minimumValue = src.minimumValue
    this.maximumValue = src.maximumValue
    this.required = src.required
    this.unique = src.unique
    this.allowedValues = src.allowedValues
    this.description = src.description
    this.mappingValues = src.mappingValues
  }

  fun copyTo(dest: VariableDefinition) {
    dest.name = name
    dest.type = type
    dest.minimumValue = minimumValue
    dest.maximumValue = maximumValue
    dest.allowedValuesList?.clear()
    allowedValues?.let {
      dest.addAllowedValues(it)
    }
    dest.description = description
    dest.isRequired = required
    dest.isUnique = unique
  }

  fun copyTo(dest: DependentVariableDefinition) {
    dest.name = name

    val destMapping = mutableMapOf<Any, Any?>()
    val values = CSVStringUtils.parseStringList(mappingValues)
    dependsOn?.let {
      it.allowedValues?.forEachIndexed { index, masterValue ->
        destMapping[masterValue] = values.getOrNull(index)
      }
      dest.mapping = destMapping
      val master = VariableDefinition()
      it.copyTo(master)
      dest.dependsOn = master
    }
  }

  companion object {
    fun from(definition: VariableDefinition): MerlinVariable {
      val variable = MerlinVariable()
      variable.name = definition.name
      variable.defined = true
      variable.description = definition.description
      variable.required = definition.isRequired
      variable.unique = definition.isUnique
      variable.minimumValue = definition.minimumValue
      variable.maximumValue = definition.maximumValue
      variable.allowedValues = definition.allowedValuesList?.map { "$it" }
      variable.type = definition.type
      return variable
    }

    fun from(definition: DependentVariableDefinition): MerlinVariable {
      val variable = MerlinVariable()
      variable.name = definition.name
      variable.mappingValues = definition.mapping?.values?.joinToString { "$it" }
      variable.dependsOn = from(definition.dependsOn)
      return variable
    }
  }
}
