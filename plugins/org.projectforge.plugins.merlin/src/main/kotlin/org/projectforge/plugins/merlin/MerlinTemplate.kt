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

import org.projectforge.framework.jcr.Attachment
import org.projectforge.rest.JsonUtils
import org.projectforge.rest.dto.AttachmentsSupport
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User

class MerlinTemplate(
  id: Int? = null,
  var name: String? = null,
  var description: String? = null,
  var admins: List<User>? = null,
  var adminsAsString: String? = null,
  var accessGroups: List<Group>? = null,
  var accessGroupsAsString: String? = null,
  var accessGroupIds: String? = null,
  var accessUsers: List<User>? = null,
  var accessUsersAsString: String? = null,
  var fileNamePattern: String? = null,
  var stronglyRestrictedFilenames: Boolean? = null,
  var pdfExport: Boolean? = null,
  var wordTemplateFileName: String? = null,
  /**
   * Used by [org.projectforge.plugins.merlinMerlinVariablePageRest]
   */
  var currentVariable: MerlinVariable? = null,
  override var attachmentsCounter: Int? = null,
  override var attachmentsSize: Long? = null,
) : BaseDTO<MerlinTemplateDO>(id), AttachmentsSupport {
  override var attachments: List<Attachment>? = null

  var variables = mutableListOf<MerlinVariable>()

  var dependentVariables = mutableListOf<MerlinVariable>()


  // The user and group ids are stored as csv list of integers in the data base.
  override fun copyFrom(src: MerlinTemplateDO) {
    super.copyFrom(src)
    admins = User.toUserList(src.adminIds)
    accessGroups = Group.toGroupList(src.accessGroupIds)
    accessUsers = User.toUserList(src.accessUserIds)
    variablesFromJson(src.variables)?.let { variables = it }
    variablesFromJson(src.dependentVariables)?.let { dependentVariables = it }
  }

  private fun variablesFromJson(json: String?): MutableList<MerlinVariable>? {
    if (json.isNullOrBlank()) {
      return null
    }
    JsonUtils.fromJson(json, Array<MerlinVariable>::class.java, failOnUnknownProps = false)?.let {
      return it.toMutableList()
    }
    return null
  }

  // The user and group ids are stored as csv list of integers in the data base.
  override fun copyTo(dest: MerlinTemplateDO) {
    super.copyTo(dest)
    dest.adminIds = User.toIntList(admins)
    dest.accessGroupIds = Group.toIntList(accessGroups)
    dest.accessUserIds = User.toIntList(accessUsers)
    dest.variables = variablesToJson(variables)
    dest.dependentVariables = variablesToJson(dependentVariables)
  }

  private fun variablesToJson(variables: MutableList<MerlinVariable>): String? {
    if (variables.isNullOrEmpty()) {
      return null
    }
    val baseVariables = variables.map { MerlinVariableBase().copyFrom(it) }
    return JsonUtils.toJson(baseVariables, ignoreNullableProps = true)
  }

  /**
   * Reorders variables ([variables] and [dependentVariables]), if any state of any variable was changed.
   */
  fun reorderVariables() {
    val allVariables = variables + dependentVariables
    replaceVariables(allVariables)
  }

  fun replaceVariables(allVariables: List<MerlinVariable>) {
    variables = extractInputVariables(allVariables).toMutableList()
    dependentVariables = extractDependentVariables(allVariables).toMutableList()
  }

  companion object {
    fun extractInputVariables(allVariables: List<MerlinVariable>): List<MerlinVariable> {
      return allVariables.filter { !it.dependent }
    }

    fun extractDependentVariables(allVariables: List<MerlinVariable>): List<MerlinVariable> {
      return allVariables.filter { it.dependent }.sortedBy { it.name.toLowerCase() }
    }
  }
}
