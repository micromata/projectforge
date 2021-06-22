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

import mu.KotlinLogging
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.plugins.merlin.*
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

private val log = KotlinLogging.logger {}

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
  private lateinit var merlinRunner: MerlinRunner

  @Autowired
  private lateinit var userService: UserService

  @PostConstruct
  private fun postConstruct() {
    enableJcr()
    instance = this
  }

  /**
   * Initializes new toDos for adding.
   */
  override fun newBaseDO(request: HttpServletRequest?): MerlinTemplateDO {
    val template = super.newBaseDO(request)
    template.adminIds = "${ThreadLocalUserContext.getUserId()}"
    template.fileNamePattern = "\${date}-document"
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
          .add(UITableColumn("attachmentsSizeFormatted", titleIcon = UIIconType.PAPER_CLIP))
          .add(UITableColumn("adminsAsString", "plugins.merlin.admins"))
          .add(UITableColumn("accessUsersAsString", "plugins.merlin.accessUsers"))
          .add(UITableColumn("accessGroupsAsString", "plugins.merlin.accessGroups"))
      )
      .add(
        UIAlert(
          "'For further documentation, please refer: [Merlin documentation](https://github.com/micromata/Merlin/blob/master/docs/templates.adoc)",
          markdown = true
        )
      )
    /*layout.add(
      MenuItem(
        "HIGHLIGHT",
        i18nKey = "plugins.merlin.personalBox",
        tooltip = "plugins.merlin.personalBox.info",
        url = PagesResolver.getDynamicPageUrl(merlinPersonalBoxPageRest::class.java)
      )
    )*/
    return LayoutUtils.processListPage(layout, this)
  }

  /**
   * LAYOUT Edit page
   */
  override fun createEditLayout(dto: MerlinTemplate, userAccess: UILayout.UserAccess): UILayout {
    return createEditLayout(null, dto, userAccess)
  }

  private fun getUserAccess(dbo: MerlinTemplateDO): UILayout.UserAccess {
    val userAccess = UILayout.UserAccess()
    super.checkUserAccess(dbo, userAccess)
    return userAccess
  }

  private fun createEditLayoutSuper(dto: MerlinTemplate, userAccess: UILayout.UserAccess): UILayout {
    return super.createEditLayout(dto, userAccess)
  }

  /**
   * @param id Id of the MerlinTemplateDO
   */
  private fun getStatistics(id: Int): MerlinStatistics {
    val list =
      attachmentsService.getAttachments(jcrPath!!, id, attachmentsAccessChecker)?.sortedByDescending { it.created }
        ?: return MerlinStatistics()
    val wordTemplate = list.find { it.fileExtension == "docx" } ?: return MerlinStatistics()

    attachmentsService.getAttachmentInputStream(
      jcrPath!!,
      id,
      wordTemplate.fileId!!,
      accessChecker = attachmentsAccessChecker,
    )?.let {
      val istream = it.second
      val fileObject = it.first
      istream.use {
        val stats = merlinRunner.analyzeWordDocument(istream, fileObject.fileName ?: "untitled.docx")
        log.info("Statistics: $stats")
        return stats
      }
    }
    return MerlinStatistics()
  }

  companion object {
    private lateinit var instance: MerlinPagesRest

    internal fun createEditLayout(
      dbo: MerlinTemplateDO? = null,
      dto: MerlinTemplate = instance.transformFromDB(dbo!!),
      userAccess: UILayout.UserAccess? = null
    ): UILayout {
      check(dbo != null || userAccess != null) { "dbo or userAcess must be given." }
      val stats = instance.getStatistics(dbo?.id ?: dto.id!!)
      val inputVariables = UIBadgeList()
      stats.variables.sortedBy { it.name.toLowerCase() }.forEach {
        if (it.input) {
          inputVariables.add(UIBadge(it.name, it.uiColor))
        }
      }
      val dependentVariables = UIBadgeList()
      stats.variables.sortedBy { it.name.toLowerCase() }.forEach {
        inputVariables.add(UIBadge(it.name, it.uiColor))
      }
      val lc = LayoutContext(MerlinTemplateDO::class.java)
      val adminsSelect = UISelect.createUserSelect(
        lc,
        "admins",
        true,
        "plugins.merlin.admins",
        tooltip = "plugins.merlin.admins.info"
      )
      val accessUsers = UISelect.createUserSelect(
        lc,
        "accessUsers",
        true,
        "plugins.merlin.accessUsers",
        tooltip = "plugins.merlin.accessUsers.info"
      )
      val accessGroups = UISelect.createGroupSelect(
        lc,
        "accessGroups",
        true,
        "plugins.merlin.accessGroups",
        tooltip = "plugins.merlin.accessGroups.info"
      )
      val layout = instance.createEditLayoutSuper(dto, userAccess ?: instance.getUserAccess(dbo!!))
        .add(
          UIFieldset(UILength(md = 12, lg = 12))
            .add(lc, "name")
            .add(
              UIRow()
                .add(
                  UICol()
                    .add(UIInput("fileNamePattern", lc))
                )
                .add(
                  UICol()
                    .add(lc, "stronglyRestrictedFilenames")
                )
            )
            .add(lc, "description")
            .add(UILabel("plugins.merlin.variables.input", tooltip = "plugins.merlin.variables.input.info"))
            .add(inputVariables)
        )
        .add(
          UIFieldset(UILength(md = 12, lg = 12), title = "access.title.heading")
            .add(
              UIRow()
                .add(
                  UICol(UILength(md = 4))
                    .add(adminsSelect)
                )
                .add(
                  UICol(UILength(md = 4))
                    .add(accessUsers)
                )
                .add(
                  UICol(UILength(md = 4))
                    .add(accessGroups)
                )
            )
        )
        .add(
          UIFieldset(title = "attachment.list")
            .add(UIAttachmentList(instance.category, dto.id))
        )
      return LayoutUtils.processEditPage(layout, dto, instance)
    }
  }
}
