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

import com.fasterxml.jackson.annotation.JsonIgnore
import de.micromata.merlin.csv.CSVStringUtils
import de.micromata.merlin.word.templating.VariableType
import org.projectforge.common.anots.PropertyInfo

/**
 * For serializing/deserializing with only minimum properties editable by the user.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
open class MerlinVariableBase : Comparable<MerlinVariableBase> {
  @PropertyInfo(i18nKey = "plugins.merlin.variable.name")
  var name: String = ""

  @PropertyInfo(i18nKey = "plugins.merlin.variable.sortName", tooltip = "plugins.merlin.variable.sortName.info")
  var sortName: String? = null

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

  @PropertyInfo(i18nKey = "plugins.merlin.variable.scale", additionalI18nKey = "plugins.merlin.variable.scale.info")
  var scale: Int? = null

  @PropertyInfo(
    i18nKey = "plugins.merlin.variable.allowedValues",
    additionalI18nKey = "plugins.merlin.variable.allowedValues.info"
  )
  var allowedValues: List<String>? = null

  @PropertyInfo(i18nKey = "plugins.merlin.variable.type")
  var type: VariableType = VariableType.STRING

  @PropertyInfo(i18nKey = "plugins.merlin.variable.dependsOn")
  var dependsOnName: String? = null

  @PropertyInfo(i18nKey = "plugins.merlin.variable.mapping", additionalI18nKey = "plugins.merlin.variable.mapping.info")
  var mappingValues: String? = null

  @get:JsonIgnore
  val mappingValuesArray: Array<out String>
    get() = CSVStringUtils.parseStringList(mappingValues)

  /**
   * Compares sortName/name (ignoring case).
   */
  override fun compareTo(other: MerlinVariableBase): Int {
    return (this.sortName ?: this.name).compareTo(other.sortName ?: other.name, ignoreCase = true)
  }

  /**
   * @return this for chaining.
   */
  fun copyFrom(src: MerlinVariableBase): MerlinVariableBase {
    this.name = src.name
    this.sortName = src.sortName
    this.type = src.type
    this.minimumValue = src.minimumValue
    this.maximumValue = src.maximumValue
    this.scale = src.scale
    this.required = src.required
    this.unique = src.unique
    this.allowedValues = src.allowedValues
    this.description = src.description
    this.mappingValues = src.mappingValues
    this.dependsOnName = src.dependsOnName
    return this
  }
}
