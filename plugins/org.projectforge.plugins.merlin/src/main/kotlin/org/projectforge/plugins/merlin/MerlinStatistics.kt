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
import de.micromata.merlin.word.templating.Template
import de.micromata.merlin.word.templating.TemplateDefinition
import de.micromata.merlin.word.templating.TemplateStatistics
import org.projectforge.framework.ToStringUtil

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MerlinStatistics {
  val variables = mutableListOf<MerlinVariable>()
  var wordTemplateFilename: String? = null
  var excelTemplateDefinitionFilename: String? = null

  @get:JsonIgnore
  var templateStatistics: TemplateStatistics? = null

  @get:JsonIgnore
  var templateDefinition: TemplateDefinition? = null

  @get:JsonIgnore
  var template: Template? = null

  fun update(statistics: TemplateStatistics? = null, templateDefinition: TemplateDefinition? = null) {
    this.templateStatistics = statistics
    this.templateDefinition = templateDefinition
    synchronized(variables) {
      variables.clear()
      statistics?.inputVariables?.forEach { variableDefinition ->
        val name = variableDefinition.name
        val variable = MerlinVariable(
          name,
          variableDefinition,
          used = statistics.usedVariables?.contains(name) == true,
          masterVariable = statistics.masterVariables?.contains(name) == true
        )
        variables.add(variable)
      }
      templateDefinition?.dependentVariableDefinitions?.forEach { variableDefinition ->
        val name = variableDefinition.name
        val variable = MerlinVariable(
          name,
          dependentVariableDefinition = variableDefinition,
          used = statistics?.usedVariables?.contains(name) == true
        )
        variables.add(variable)
      }
    }
  }

  override fun toString(): String {
    return ToStringUtil.toJsonString(this)
  }
}
