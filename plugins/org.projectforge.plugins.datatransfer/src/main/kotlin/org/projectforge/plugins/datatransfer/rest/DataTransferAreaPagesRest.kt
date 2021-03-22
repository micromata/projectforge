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

import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.i18n.translate
import org.projectforge.plugins.datatransfer.DataTransferAreaDO
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestButtonEvent
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@RestController
@RequestMapping("${Rest.URL}/datatransfer")
class DataTransferAreaPagesRest : AbstractDTOPagesRest<DataTransferAreaDO, DataTransferArea, DataTransferAreaDao>(
  DataTransferAreaDao::class.java,
  "plugins.datatransfer.title"
) {

  @Autowired
  private lateinit var groupService: GroupService

  @Autowired
  private lateinit var userService: UserService

  @PostConstruct
  private fun postConstruct() {
    /**
     * Enable attachments for this entity.
     */
    enableJcr(baseDao.maxFileSize.toBytes(), DataTransferAreaDao.MAX_FILE_SIZE_SPRING_PROPERTY)
  }

  override fun transformForDB(dto: DataTransferArea): DataTransferAreaDO {
    val obj = DataTransferAreaDO()
    dto.copyTo(obj)
    return obj
  }

  override fun transformFromDB(obj: DataTransferAreaDO, editMode: Boolean): DataTransferArea {
    val dto = DataTransferArea()
    dto.copyFrom(obj)
    dto.externalLinkBaseUrl = baseDao.getExternalBaseLinkUrl()

    // Group names needed by React client (for ReactSelect):
    Group.restoreDisplayNames(dto.accessGroups, groupService)

    // Usernames needed by React client (for ReactSelect):
    User.restoreDisplayNames(dto.admins, userService)
    User.restoreDisplayNames(dto.accessUsers, userService)

    return dto
  }

  /**
   * @return the address view page.
   */
  override fun getStandardEditPage(): String {
    return "${PagesResolver.getDynamicPageUrl(DataTransferPageRest::class.java)}:id"
  }

  /**
   * Initializes new DataTransferFiles for adding.
   */
  override fun newBaseDO(request: HttpServletRequest?): DataTransferAreaDO {
    return baseDao.createInitializedFile()
  }

  @PostMapping("renewAccessToken")
  fun renewAccessToken(@Valid @RequestBody postData: PostData<DataTransferArea>): ResponseAction {
    val file = postData.data
    file.externalAccessToken = DataTransferAreaDao.generateExternalAccessToken()
    return ResponseAction(targetType = TargetType.UPDATE)
      .addVariable("data", file)
  }

  @PostMapping("renewPassword")
  fun renewPassword(@Valid @RequestBody postData: PostData<DataTransferArea>): ResponseAction {
    val file = postData.data
    file.externalPassword = DataTransferAreaDao.generateExternalPassword()
    return ResponseAction(targetType = TargetType.UPDATE)
      .addVariable("data", file)
  }

  /**
   * LAYOUT List page
   */
  override fun createListLayout(): UILayout {
    val layout = super.createListLayout()
      .add(
        UITable.createUIResultSetTable()
          .add(lc, "created", "lastUpdate", "areaName", "description")
          .add(UITableColumn("attachmentsSize", titleIcon = UIIconType.PAPER_CLIP))
      )
    return LayoutUtils.processListPage(layout, this)
  }

  override fun afterOperationRedirectTo(
    obj: DataTransferAreaDO,
    postData: PostData<DataTransferArea>,
    event: RestButtonEvent
  ): String? {
    return if (event == RestButtonEvent.SAVE) PagesResolver.getDynamicPageUrl(
      DataTransferPageRest::class.java,
      id = obj.id,
      absolute = true
    ) else null
  }

  /**
   * LAYOUT Edit page
   */
  override fun createEditLayout(dto: DataTransferArea, userAccess: UILayout.UserAccess): UILayout {
    val adminsSelect = UISelect.createUserSelect(
      lc,
      "admins",
      true,
      "plugins.datatransfer.admins",
      tooltip = "plugins.datatransfer.admins.info"
    )
    val accessUsers = UISelect.createUserSelect(
      lc,
      "accessUsers",
      true,
      "plugins.datatransfer.accessUsers",
      tooltip = "plugins.datatransfer.accessUsers.info"
    )
    val accessGroups = UISelect.createGroupSelect(
      lc,
      "accessGroups",
      true,
      "plugins.datatransfer.accessGroups",
      tooltip = "plugins.datatransfer.accessGroups.info"
    )
    val resetExternalPassword = UIButton(
      "accessPassword-renew",
      title = translate("plugins.datatransfer.external.password.renew"),
      tooltip = "plugins.datatransfer.external.password.renew.info",
      color = UIColor.DANGER,
      responseAction = ResponseAction(
        RestResolver.getRestUrl(this::class.java, "renewPassword"),
        targetType = TargetType.POST
      )
    )
    val externalLink = UIReadOnlyField(
      "externalLink",
      lc,
      label = "plugins.datatransfer.external.link",
      canCopy = true
    )
    val renewExternalLink = UIButton(
      "accessToken-renew",
      title = translate("plugins.datatransfer.external.link.renew"),
      tooltip = "plugins.datatransfer.external.link.renew.info",
      color = UIColor.DANGER,
      responseAction = ResponseAction(RestResolver.getRestUrl(this::class.java, "renewAccessToken"), targetType = TargetType.POST)
    )
    val externalAccessFieldset =
      UIFieldset(UILength(md = 12, lg = 12), title = "plugins.datatransfer.external.access.title")

    val layout = super.createEditLayout(dto, userAccess)
      .add(
        UIFieldset(UILength(md = 12, lg = 12))
          .add(lc, "areaName", "description")
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
        externalAccessFieldset
          .add(
            UIRow()
              .add(
                UICol(UILength(md = 6))
                  .add(lc, "externalDownloadEnabled")
              )
              .add(
                UICol(UILength(md = 6))
                  .add(lc, "externalUploadEnabled")
              )
          )
      )
    //if (dto.externalDownloadEnabled == true || dto.externalUploadEnabled == true) {
    externalAccessFieldset.add(
      UIRow()
        .add(
          UICol(UILength(md = 6))
            .add(
              UIRow()
                .add(
                  UICol(8)
                    .add(lc, "externalPassword")
                )
                .add(
                  UICol(4)
                    .add(resetExternalPassword)
                )
            )
        )
    )
      .add(
        UIRow()
          .add(
            UICol(10)
              .add(externalLink)
          )
          .add(
            UICol(2)
              .add(renewExternalLink)
          )
      )

    //}
    layout.getInputById("areaName").focus = true
    //layout.watchFields.addAll(arrayOf("externalDownloadEnabled", "externalUploadEnabled"))

    return LayoutUtils.processEditPage(layout, dto, this)
  }

  /*
  override fun onWatchFieldsUpdate(
    request: HttpServletRequest,
    dto: DataTransferArea,
    watchFieldsTriggered: Array<String>?
  ): ResponseEntity<ResponseAction> {
    val layout =
      createEditLayout(dto, UILayout.UserAccess(history = false, insert = true, update = true, delete = true))
    return ResponseEntity.ok(ResponseAction(targetType = TargetType.UPDATE).addVariable("ui", layout))
  }*/
}
