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

package org.projectforge.plugins.merlin.rest

import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.service.UserService
import org.projectforge.plugins.merlin.MerlinTemplate
import org.projectforge.plugins.merlin.MerlinTemplateDO
import org.projectforge.plugins.merlin.MerlinTemplateDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/merlin")
class MerlinPagesRest :
  AbstractDTOPagesRest<MerlinTemplateDO, MerlinTemplate, MerlinTemplateDao>(
    MerlinTemplateDao::class.java,
    "plugins.merlin.title"
  ) {
  @Autowired
  private lateinit var groupService: GroupService

  @Autowired
  private lateinit var userService: UserService

  @PostConstruct
  private fun postConstruct() {
    enableJcr()
  }

  /**
   * Initializes new toDos for adding.
   */
  override fun newBaseDO(request: HttpServletRequest?): MerlinTemplateDO {
    val template = super.newBaseDO(request)
    return template
  }

  override fun transformForDB(dto: MerlinTemplate): MerlinTemplateDO {
    val obj = MerlinTemplateDO()
    dto.copyTo(obj)
    return obj
  }

  override fun transformFromDB(obj: MerlinTemplateDO, editMode: Boolean): MerlinTemplate {
    val dto = MerlinTemplate()
    dto.copyFrom(obj)

    // Group names needed by React client (for ReactSelect):
    Group.restoreDisplayNames(dto.accessGroups, groupService)

    // Usernames needed by React client (for ReactSelect):
    User.restoreDisplayNames(dto.admins, userService)
    User.restoreDisplayNames(dto.accessUsers, userService)

    dto.adminsAsString = dto.admins?.joinToString { it.displayName ?: "???" } ?: ""
    dto.accessGroupsAsString = dto.accessGroups?.joinToString { it.displayName ?: "???" } ?: ""
    dto.accessUsersAsString = dto.accessUsers?.joinToString { it.displayName ?: "???" } ?: ""
    return dto
  }

  /**
   * LAYOUT List page
   */
  override fun createListLayout(): UILayout {
    val layout = super.createListLayout()
      .add(
        UITable.createUIResultSetTable()
          .add(lc, "created", "modified", "name", "description")
          .add(UITableColumn("adminsAsString", "plugins.datatransfer.admins"))
          .add(UITableColumn("accessGroupsAsString", "plugins.datatransfer.accessGroups"))
          .add(UITableColumn("accessUsersAsString", "plugins.datatransfer.accessUsers"))
      )
    return LayoutUtils.processListPage(layout, this)
  }

  /**
   * LAYOUT Edit page
   */
  override fun createEditLayout(dto: MerlinTemplate, userAccess: UILayout.UserAccess): UILayout {
    val layout = super.createEditLayout(dto, userAccess)
      .add(lc, "subject")
      .add(
        UIRow()
          .add(
            UICol()
              .add(lc, "type", "status", "dueDate")
          )
          .add(
            UICol()
              .add(lc, "priority", "assignee", "reporter")
          )
      )
      .add(lc, "task")
      .add(lc, "description", "comment", "options")
    return LayoutUtils.processEditPage(layout, dto, this)
  }
}
