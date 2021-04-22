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
import org.projectforge.common.FormatterUtils
import org.projectforge.framework.configuration.ConfigurationChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.menu.MenuItem
import org.projectforge.plugins.datatransfer.DataTransferAccessChecker
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
import org.springframework.http.ResponseEntity
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
  private lateinit var configurationChecker: ConfigurationChecker

  @Autowired
  private lateinit var groupService: GroupService

  @Autowired
  private lateinit var userService: UserService

  @PostConstruct
  private fun postConstruct() {
    enableJcr(
      attachmentsAccessChecker = DataTransferAccessChecker(baseDao)
    )
  }

  override fun transformForDB(dto: DataTransferArea): DataTransferAreaDO {
    val obj = DataTransferAreaDO()
    dto.copyTo(obj)
    return obj
  }

  override fun transformFromDB(obj: DataTransferAreaDO, editMode: Boolean): DataTransferArea {
    return DataTransferArea.transformFromDB(obj, baseDao, groupService, userService)
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
          .add(lc, "created")
          .add(UITableColumn("lastUpdateTimeAgo", "lastUpdate"))
          .add(lc, "areaName", "description")
          .add(UITableColumn("attachmentsSizeFormatted", titleIcon = UIIconType.PAPER_CLIP))
          .add(UITableColumn("maxUploadSizeFormatted", "plugins.datatransfer.maxUploadSize"))
          .add(UITableColumn(
              "externalAccessEnabled",
              "plugins.datatransfer.external.access.title"
            ).setStandardBoolean()
          )
          .add(lc, "expiryDays")
          .add(UITableColumn("adminsAsString", "plugins.datatransfer.admins"))
          .add(UITableColumn("observersAsString", "plugins.datatransfer.observers"))
          .add(UITableColumn("accessGroupsAsString", "plugins.datatransfer.accessGroups"))
          .add(UITableColumn("accessUsersAsString", "plugins.datatransfer.accessUsers"))
      )
    layout.add(
      MenuItem(
        "personalBox",
        i18nKey = "plugins.datatransfer.personalBox",
        tooltip = "plugins.datatransfer.personalBox.info",
        url = PagesResolver.getDynamicPageUrl(DataTransferPersonalBoxPageRest::class.java)
      )
    )
    return LayoutUtils.processListPage(layout, this)
  }

  override fun preProcessMagicFilter(
    target: QueryFilter,
    source: MagicFilter
  ): List<CustomResultFilter<DataTransferAreaDO>>? {
    source.sortProperties.find { it.property == "lastUpdateTimeAgo" }?.property = "lastUpdate"
    return super.preProcessMagicFilter(target, source)
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

  override fun validate(validationErrors: MutableList<ValidationError>, dto: DataTransferArea) {
    if (dto.externalAccessEnabled) {
      if (!NumberHelper.checkSecureRandomAlphanumeric(
          dto.externalAccessToken,
          DataTransferAreaDao.ACCESS_TOKEN_LENGTH
        )
      ) {
        validationErrors.add(
          ValidationError(translate("plugins.datatransfer.validation.error.token"))
        )
      }
      if (dto.externalPassword?.trim()?.length ?: 0 < 6) {
        validationErrors.add(
          ValidationError(
            translate("plugins.datatransfer.validation.error.password"), fieldId = "externalPassword"
          )
        )
      }
    }
    if (!DataTransferAreaDao.EXPIRY_DAYS_VALUES.containsKey(dto.expiryDays)) {
      validationErrors.add(
        ValidationError(
          translate("plugins.datatransfer.validation.error.expiryDays"), fieldId = "expiryDays"
        )
      )
    }
    if (!DataTransferAreaDao.MAX_UPLOAD_SIZE_VALUES.contains(dto.maxUploadSizeKB)) {
      validationErrors.add(
        ValidationError(
          translate("plugins.datatransfer.validation.error.maxUploadSizeKB"), fieldId = "maxUploadSizeKB"
        )
      )
    }
    dto.maxUploadSizeKB?.let {
      val springServletMultipartMaxFileSize = configurationChecker.springServletMultipartMaxFileSize.toBytes()
      if (1024L * it > springServletMultipartMaxFileSize) {
        validationErrors.add(
          ValidationError(
            translateMsg(
              "plugins.datatransfer.validation.error.maxUploadSizeKB.exceededGlobalMaxUploadSize",
              FormatterUtils.formatBytes(springServletMultipartMaxFileSize)
            ), fieldId = "maxUploadSizeKB"
          )
        )
      }
    }
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
    val observersSelect = UISelect.createUserSelect(
      lc,
      "observers",
      true,
      "plugins.datatransfer.observers",
      tooltip = "plugins.datatransfer.observers.info"
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
      responseAction = ResponseAction(
        RestResolver.getRestUrl(this::class.java, "renewAccessToken"),
        targetType = TargetType.POST
      )
    )
    val externalAccessFieldset =
      UIFieldset(UILength(md = 12, lg = 12), title = "plugins.datatransfer.external.access.title")

    val expiryDaysSelectValues =
      DataTransferAreaDao.EXPIRY_DAYS_VALUES.map { UISelectValue(it.key, translateMsg(it.value, it.key)) }

    val maxUploadSizeKBValues =
      DataTransferAreaDao.MAX_UPLOAD_SIZE_VALUES.map { UISelectValue(it, FormatterUtils.formatBytes(1024L * it)) }

    val layout = super.createEditLayout(dto, userAccess)
      .add(
        UIFieldset(UILength(md = 12, lg = 12))
          .add(
            UIRow().add(
              UICol(UILength(md = 8))
                .add(lc, "areaName")
            )
              .add(
                UICol(UILength(md = 4))
                  .add(
                    UISelect(
                      "expiryDays",
                      values = expiryDaysSelectValues,
                      label = "plugins.datatransfer.expiryDays",
                      tooltip = "plugins.datatransfer.expiryDays.info"
                    )
                  )
              )
          )
          .add(
            UIRow().add(
              UICol(UILength(md = 8))
                .add(observersSelect)
            )
              .add(
                UICol(UILength(md = 4))
                  .add(
                    UISelect(
                      "maxUploadSizeKB",
                      values = maxUploadSizeKBValues,
                      label = "plugins.datatransfer.maxUploadSize",
                      tooltip = "plugins.datatransfer.maxUploadSize.info"
                    )
                  )
              )
          )
          .add(lc, "description")
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
    if (dto.externalDownloadEnabled == true || dto.externalUploadEnabled == true) {
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

    }
    layout.getInputById("areaName").focus = true
    layout.watchFields.addAll(arrayOf("externalDownloadEnabled", "externalUploadEnabled"))
    dto.layoutUid = layout.uid

    return LayoutUtils.processEditPage(layout, dto, this)
  }


  override fun onWatchFieldsUpdate(
    request: HttpServletRequest,
    dto: DataTransferArea,
    watchFieldsTriggered: Array<String>?
  ): ResponseEntity<ResponseAction> {
    val preserveLayoutUid = dto.layoutUid
    val layout =
      createEditLayout(dto, UILayout.UserAccess(history = false, insert = true, update = true, delete = true))
    preserveLayoutUid?.let {
      layout.uid = it
    }
    return ResponseEntity.ok(ResponseAction(targetType = TargetType.UPDATE).addVariable("ui", layout))
  }
}
