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

package org.projectforge.plugins.merlin

import com.fasterxml.jackson.annotation.JsonProperty
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
@Suppress("unused")
class MerlinVariable : MerlinVariableBase() {
  var id: Int? = null

  var defined: Boolean = false

  /**
   * If this variable depends on another (master) variable, the master variable is given here.
   */
  var dependsOn: MerlinVariable? = null
    set(value) {
      field = value
      dependsOnName = value?.name
    }

  @get:JsonProperty
  val mappingMasterValues
    get() = writeAsCSV(dependsOn?.allowedValues)

  @get:JsonProperty
  val allowedValuesFormatted: String?
    get() = writeAsCSV(allowedValues)

  /**
   * Is this variable used in the Word template?
   */
  @PropertyInfo(i18nKey = "plugins.merlin.variable.used", additionalI18nKey = "plugins.merlin.variable.used.info")
  var used: Boolean? = null

  /**
   * Is this variable a master variables (where others depends on)?
   */
  @PropertyInfo(i18nKey = "plugins.merlin.variable.master", additionalI18nKey = "plugins.merlin.variable.master.info")
  var masterVariable: Boolean? = null

  val dependent: Boolean
    get() = dependsOn != null

  /**
   * Input variables will be displayed for editing (or given in serial Excel). Input variables are defined and not dependent.
   * Equals to definded && !dependent.
   */
  val input: Boolean
    get() = defined && !dependent

  val uiColor: UIColor?
    get() {
      return when {
        masterVariable == true -> {
          UIColor.DANGER
        }
        used == false -> {
          UIColor.LIGHT
        }
        input -> {
          UIColor.SUCCESS
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
            writeAsCSV(list)
          )
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
    super.copyFrom(src)
    this.dependsOn = src.dependsOn
    this.id = src.id
    this.defined = src.defined
    this.used = src.used
    this.masterVariable = src.masterVariable
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
    val values = mappingValuesArray
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

  fun copyFrom(src: VariableDefinition) {
    this.name = src.name
    this.defined = true
    this.description = src.description
    this.required = src.isRequired
    this.unique = src.isUnique
    this.minimumValue = src.minimumValue
    this.maximumValue = src.maximumValue
    this.allowedValues = src.allowedValuesList?.map { "$it" }
    this.type = src.type
  }

  fun copyFrom(src: DependentVariableDefinition) {
    this.name = src.name
    this.mappingValues = writeAsCSV(src.mapping?.values)
    this.dependsOn = from(src.dependsOn)
  }

  companion object {
    fun from(definition: VariableDefinition): MerlinVariable {
      val variable = MerlinVariable()
      variable.copyFrom(definition)
      return variable
    }

    fun from(definition: DependentVariableDefinition): MerlinVariable {
      val variable = MerlinVariable()
      variable.copyFrom(definition)
      return variable
    }

    internal fun writeAsCSV(values: Collection<Any?>?): String {
      values ?: return ""
      val sb = StringBuilder()
      var first = true
      values.forEach {
        if (!first) {
          sb.append(", ")
        }
        val str = it?.toString() ?: ""
        if (str.contains(',') || str.trim() != it) {
          sb.append("\"$str\"")
        } else {
          sb.append(str)
        }
        first = false
      }
      return sb.toString()
    }
  }
}
