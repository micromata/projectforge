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

import com.fasterxml.jackson.annotation.JsonIgnore
import de.micromata.merlin.persistency.FileDescriptor
import de.micromata.merlin.word.templating.Template
import de.micromata.merlin.word.templating.TemplateDefinition
import de.micromata.merlin.word.templating.TemplateStatistics
import de.micromata.merlin.word.templating.WordTemplateChecker
import org.projectforge.framework.ToStringUtil

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MerlinStatistics {
  val variables = mutableListOf<MerlinVariable>()
  val conditionals = mutableListOf<MerlinConditional>()

  @get:JsonIgnore
  var templateStatistics: TemplateStatistics? = null
    private set

  @get:JsonIgnore
  var templateDefinition: TemplateDefinition? = null

  @get:JsonIgnore
  var template: Template? = null

  fun update(templateChecker: WordTemplateChecker, wordFilename: String) {
    template = templateChecker.template
    template!!.fileDescriptor = createFileDescriptor(wordFilename)
    val statistics = templateChecker.template.statistics
    this.templateDefinition = templateChecker.template.templateDefinition
    this.templateStatistics = statistics
    synchronized(conditionals) {
      conditionals.clear()
      statistics?.conditionals?.conditionalsSet?.forEach {
        conditionals.add(MerlinConditional(it))
      }
    }
    synchronized(variables) {
      variables.clear()
      var counter = 0
      statistics?.inputVariables?.forEach { variableDefinition ->
        val name = variableDefinition.name
        val used = statistics.usedVariables?.contains(name) == true
        val masterVariable = statistics.masterVariables?.contains(name) == true || conditionalsUsesVariable(name)
        val variable = MerlinVariable.from(variableDefinition).with(
          id = counter++,
          name = name,
          used = used,
          masterVariable = masterVariable,
        )
        variables.add(variable)
      }
      templateDefinition?.dependentVariableDefinitions?.forEach { variableDefinition ->
        val name = variableDefinition.name
        val variable = MerlinVariable.from(variableDefinition).with(
          id = counter++,
          name = name,
          used = statistics?.usedVariables?.contains(name) == true
        )
        variables.add(variable)
      }
    }
  }

  /**
   * Updates dto variables: Adds statistics variables not yet included in dto and updates usage info (defined, master variabl etc.)
   * of dto variables and dependentVariables.
   */
  fun updateDtoVariables(dto: MerlinTemplate) {
    val statistics = templateStatistics ?: return
    dto.variables.forEach {
      it.used = statistics.usedVariables?.contains(it.name) == true
      it.masterVariable = statistics.masterVariables?.contains(it.name) == true || conditionalsUsesVariable(it.name)
    }
    dto.dependentVariables.forEach {
      it.used = statistics.usedVariables?.contains(it.name) == true
    }
    // Add missing variables in dto, but found in template.
    variables.forEach { variable ->
      if (dto.findVariableByName(variable.name) == null) {
        val newVariable = MerlinVariable()
        newVariable.copyFrom(variable)
        newVariable.id = null // Force to use own id (if variable was renamed).
        dto.variables.add(newVariable) // Add as variable, dependent variables will be re-assigned at the end of this method
                                    // by [MerlinTemplate.reorderVariables].
      }
    }
    dto.reorderVariables()
  }

  override fun toString(): String {
    return ToStringUtil.toJsonString(this)
  }

  fun conditionalsAsMarkdown(): String {
    val sb = StringBuilder()
    conditionals.forEach {
      it.asMarkDown(sb, "")
    }
    return sb.toString()
  }

  fun conditionalsUsesVariable(name: String): Boolean {
    conditionals.forEach {
      if (it.usesVariable(name)) {
        return true
      }
    }
    return false
  }

  private fun createFileDescriptor(filename: String): FileDescriptor {
    val fileDescriptor = FileDescriptor()
    fileDescriptor.filename = filename
    return fileDescriptor
  }
}
