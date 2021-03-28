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

package org.projectforge.plugins.datatransfer.rest

import mu.KotlinLogging
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/datatransferfiles")
class DataTransferPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  @Autowired
  private lateinit var dataTransferAreaPagesRest: DataTransferAreaPagesRest

  @Autowired
  private lateinit var dataTransferAreaDao: DataTransferAreaDao

  @Autowired
  private lateinit var groupService: GroupService

  @Autowired
  private lateinit var userService: UserService

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, @RequestParam("id") idString: String?): FormLayoutData {
    val id = NumberHelper.parseInteger(idString) ?: throw IllegalAccessException("Parameter id not an int.")
    val dbObj = dataTransferAreaDao.getById(id)
    val dto = DataTransferArea()
    dto.copyFrom(dbObj)
    dto.externalPassword = null
    dto.attachments = attachmentsService.getAttachments(
      dataTransferAreaPagesRest.jcrPath!!,
      id,
      dataTransferAreaPagesRest.attachmentsAccessChecker
    )
    dto.externalLinkBaseUrl = dataTransferAreaDao.getExternalBaseLinkUrl()
    dto.internalLink = getUrl(PagesResolver.getDynamicPageUrl(this::class.java, id = id))
    // Group names needed by React client (for ReactSelect):
    Group.restoreDisplayNames(dto.accessGroups, groupService)

    // Usernames needed by React client (for ReactSelect):
    User.restoreDisplayNames(dto.admins, userService)
    User.restoreDisplayNames(dto.observers, userService)
    User.restoreDisplayNames(dto.accessUsers, userService)

    dto.adminsAsString = dto.admins?.joinToString { it.displayName ?: "???" } ?: ""
    dto.observersAsString = dto.observers?.joinToString { it.displayName ?: "???" } ?: ""
    dto.accessGroupsAsString = dto.accessGroups?.joinToString { it.displayName ?: "???" } ?: ""
    dto.accessUsersAsString = dto.accessUsers?.joinToString { it.displayName ?: "???" } ?: ""
    if (!dbObj.accessGroupIds.isNullOrBlank()) {
      val accessGroupUsers =
        groupService.getGroupUsers(User.toIntArray(dbObj.accessGroupIds)).joinToString { it.displayName }
      dto.accessGroupsAsString += ": $accessGroupUsers"
    }

    val layout = UILayout("plugins.datatransfer.title.heading")
      .add(
        UIFieldset(title = "'${dbObj.areaName}")
          .add(UIAttachmentList("datatransfer", id))
      )
    layout.add(
      UIButton(
        "back",
        translate("back"),
        UIColor.SUCCESS,
        responseAction = ResponseAction(
          PagesResolver.getListPageUrl(
            DataTransferAreaPagesRest::class.java,
            absolute = true
          ), targetType = TargetType.REDIRECT
        ),
        default = true
      )
    )
    val fieldSet = UIFieldset()
      .add(
        UIRow().add(
          UICol(UILength(md = 8))
            .add(UIReadOnlyField("internalLink", label = "plugins.datatransfer.internal.link"))
        )
          .add(
            UICol(UILength(md = 4))
              .add(UIReadOnlyField("expiryDays", label = "plugins.datatransfer.expiryDays", tooltip = "plugins.datatransfer.expiryDays.info"))
          )
      )
    if (dto.externalAccessEnabled) {
      fieldSet.add(
        UIReadOnlyField(
          "externalLink",
          label = "plugins.datatransfer.external.access.title",
          dataType = UIDataType.BOOLEAN
        )
      )
    }
    fieldSet.add(UIReadOnlyField("adminsAsString", label = "plugins.datatransfer.admins"))
    if (!dto.observersAsString.isNullOrBlank()) {
      fieldSet.add(UIReadOnlyField("observersAsString", label = "plugins.datatransfer.observers"))
    }
    if (!dto.accessGroupsAsString.isNullOrBlank()) {
      fieldSet.add(UIReadOnlyField("accessGroupsAsString", label = "plugins.datatransfer.accessGroups"))
    }
    if (!dto.accessUsersAsString.isNullOrBlank()) {
      fieldSet.add(UIReadOnlyField("accessUsersAsString", label = "plugins.datatransfer.accessUsers"))
    }
    layout.add(fieldSet)

    if (dataTransferAreaDao.hasLoggedInUserUpdateAccess(dbObj, dbObj, false)) {
      layout.add(
        MenuItem(
          "EDIT",
          i18nKey = "plugins.datatransfer.title.edit",
          url = PagesResolver.getEditPageUrl(DataTransferAreaPagesRest::class.java, dto.id),
          type = MenuItemTargetType.REDIRECT
        )
      )
    }

    LayoutUtils.process(layout)
    layout.postProcessPageMenu()
    return FormLayoutData(dto, layout, createServerData(request))
  }
}
