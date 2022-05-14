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

package org.projectforge.plugins.datatransfer.rest

import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.service.UserService
import org.projectforge.common.NumberOfBytes
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.model.rest.RestPaths
import org.projectforge.plugins.datatransfer.DataTransferAreaDO
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.plugins.datatransfer.DataTransferPlugin
import org.projectforge.plugins.datatransfer.DataTransferUtils
import org.projectforge.rest.AttachmentsServicesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid

/**
 * Page of data transfer area with attachments list (including upload/download and editing).
 */
@RestController
@RequestMapping("${Rest.URL}/datatransferfiles")
class DataTransferPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  @Autowired
  private lateinit var attachmentsServicesRest: AttachmentsServicesRest

  @Autowired
  private lateinit var dataTransferAreaPagesRest: DataTransferAreaPagesRest

  @Autowired
  private lateinit var dataTransferAreaDao: DataTransferAreaDao

  @Autowired
  private lateinit var groupService: GroupService

  @Autowired
  private lateinit var userService: UserService

  @PostConstruct
  private fun postConstruct() {
    attachmentsServicesRest.register(
      dataTransferAreaPagesRest.category,
      DataTransferAttachmentsActionListener(attachmentsService, dataTransferAreaDao, groupService, userService)
    )
  }

  @GetMapping("downloadAll/{id}")
  fun downloadAll(
    @PathVariable("id", required = true) id: Int,
    response: HttpServletResponse
  ) {
    val pair = convertData(id)
    val dbObj = pair.first
    val dto = pair.second
    DataTransferRestUtils.multiDownload(
      response,
      attachmentsService,
      dataTransferAreaPagesRest.attachmentsAccessChecker,
      dbObj,
      dto.areaName,
      jcrPath = dataTransferAreaPagesRest.jcrPath!!,
      id,
      dto.attachments,
      byUser = ThreadLocalUserContext.getUser()
    )
  }

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, @RequestParam("id") idString: String?): FormLayoutData {
    var id = NumberHelper.parseInteger(idString)
    if (id == -1) {
      // personal box of logged-in user is requested:
      id = dataTransferAreaDao.ensurePersonalBox(ThreadLocalUserContext.getUserId())?.id
    }
    id ?: throw IllegalAccessException("Parameter id not an int or no personal box found.")
    val pair = convertData(id)
    val dbObj = pair.first
    val dto = pair.second
    val layout = UILayout("plugins.datatransfer.title.heading")
    val attachmentsFieldset = UIFieldset(title = "'${dto.areaName}")
    attachmentsFieldset.add(
      UIAttachmentList(
        DataTransferPlugin.ID,
        id,
        showExpiryInfo = true,
        maxSizeInKB = DataTransferAreaDao.getMaxUploadFileSizeKB(dbObj)
      )
    )
    if ((dto.attachmentsSize ?: 0) in 1..NumberOfBytes.GIGA_BYTES) {
      // Download all not for attachments with size of more than 1 GB in total.
      attachmentsFieldset.add(
        UIButton.createDownloadButton(
          id = "downloadAll",
          title = "plugins.datatransfer.button.downloadAll",
          tooltip = "plugins.datatransfer.button.downloadAll.info",
          responseAction = ResponseAction(
            RestResolver.getRestUrl(
              this.javaClass,
              "downloadAll/$id"
            ), targetType = TargetType.DOWNLOAD
          ),
          default = true
        )
      )
    }
    layout.add(attachmentsFieldset)
    layout.add(
      UIButton.createBackButton(
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
    if (dto.personalBox != true) {
      fieldSet.add(
        UIRow().add(
          UICol(UILength(md = 8))
            .add(UIReadOnlyField("observersAsString", label = "plugins.datatransfer.observers"))
        )
          .add(
            UICol(UILength(md = 4))
              .add(
                UICheckbox(
                  "userWantsToObserve",
                  label = "plugins.datatransfer.userWantsToObserve",
                  tooltip = "plugins.datatransfer.userWantsToObserve.info",
                )
              )
          )
      )
    }
    fieldSet.add(
      UIRow().add(
        UICol(UILength(md = 8))
          .add(UIReadOnlyField("internalLink", label = "plugins.datatransfer.internal.link", canCopy = true))
      )
        .add(
          UICol(UILength(md = 4))
            .add(
              UIReadOnlyField(
                "expiryDays",
                label = "plugins.datatransfer.expiryDays",
                tooltip = "plugins.datatransfer.expiryDays.info"
              )
            )
        )
    )
    if (hasEditAccess(dto, dbObj) && dto.externalAccessEnabled) {
      fieldSet.add(
        UIRow().add(
          UICol(UILength(md = 8))
            .add(UIReadOnlyField("externalLink", label = "plugins.datatransfer.external.link", canCopy = true))
        )
          .add(
            UICol(UILength(md = 4))
              .add(
                UIReadOnlyField(
                  "externalPassword",
                  label = "plugins.datatransfer.external.password",
                  canCopy = true,
                  coverUp = true
                )
              )
          )
      )
    }
    fieldSet.add(
      UIRow().add(
        UICol(UILength(md = 8))
          .add(UIReadOnlyField("adminsAsString", label = "plugins.datatransfer.admins"))
      )
        .add(
          UICol(UILength(md = 4))
            .add(
              UIReadOnlyField(
                "capacity.maxUploadSizeFormatted",
                label = "plugins.datatransfer.maxUploadSize",
                tooltip = "plugins.datatransfer.maxUploadSize.info"
              )
            )
            .add(
              UIReadOnlyField(
                "capacity.capacityAsMessage",
                label = "plugins.datatransfer.capacity",
              )
            )
        )
    )
    if (dto.personalBox != true) {
      if (!dto.accessUsersAsString.isNullOrBlank()) {
        fieldSet.add(UIReadOnlyField("accessUsersAsString", label = "plugins.datatransfer.accessUsers"))
      }
      if (!dto.accessGroupsAsString.isNullOrBlank()) {
        fieldSet.add(UIReadOnlyField("accessGroupsAsString", label = "plugins.datatransfer.accessGroups"))
      }
      if (dto.externalAccessEnabled) {
        fieldSet.add(
          UIRow().add(
            UICol(UILength(md = 6))
              .add(
                UIReadOnlyField(
                  "externalDownloadEnabled",
                  label = "plugins.datatransfer.external.download.enabled",
                  tooltip = "plugins.datatransfer.external.download.enabled",
                  dataType = UIDataType.BOOLEAN,
                )
              )
          ).add(
            UICol(UILength(md = 6))
              .add(
                UIReadOnlyField(
                  "externalUploadEnabled",
                  label = "plugins.datatransfer.external.upload.enabled",
                  tooltip = "plugins.datatransfer.external.upload.enabled",
                  dataType = UIDataType.BOOLEAN,
                )
              )
          )
        )
      }
    }

    layout.add(fieldSet)

    layout.add(
      MenuItem(
        "HIGHLIGHT",
        i18nKey = "plugins.datatransfer.audit.display",
        url = PagesResolver.getDynamicPageUrl(DataTransferAuditPageRest::class.java, id = dto.id),
        type = MenuItemTargetType.MODAL
      )
    )
    if (hasEditAccess(dto, dbObj)) {
      layout.add(
        MenuItem(
          "EDIT",
          i18nKey = "plugins.datatransfer.title.edit",
          url = PagesResolver.getEditPageUrl(DataTransferAreaPagesRest::class.java, dto.id),
          type = MenuItemTargetType.REDIRECT
        )
      )
    }
    layout.watchFields.addAll(arrayOf("userWantsToObserve"))

    LayoutUtils.process(layout)
    layout.postProcessPageMenu()
    return FormLayoutData(dto, layout, createServerData(request))
  }

  /**
   * Will be called, if the user wants to change his/her observeStatus.
   */
  @PostMapping(RestPaths.WATCH_FIELDS)
  fun watchFields(@Valid @RequestBody postData: PostData<DataTransferArea>): ResponseEntity<ResponseAction> {
    val id = postData.data.id ?: throw IllegalAccessException("Parameter id not given.")
    val userWantsToOserveArea =
      postData.data.userWantsToObserve ?: return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))

    val loggedInUser = ThreadLocalUserContext.getUser()
    val result = convertData(id)
    // OK, user has read access, so he/she is able to observe this area.
    val dbDto = result.second
    if (userWantsToOserveArea == isLoggedInUserObserver(dbDto, loggedInUser)) {
      // observe state of logged in user wasn't changed: do nothing.
      return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
    }
    val dbObj =
      dataTransferAreaDao.internalGetById(id) // Get entry including external access settings (see DataTransferDao#hasAccess).
    val newObservers = dbDto.observers?.toMutableList() ?: mutableListOf()
    if (postData.data.userWantsToObserve == true) {
      val user = User()
      user.copyFrom(loggedInUser)
      newObservers.add(user)
    } else {
      newObservers.removeIf { it.id == loggedInUser.id }
    }
    dbObj.observerIds = User.toIntList(newObservers)
    // InternalSave, because user must not be admin to observe this area. Read access is given, because data transfer
    // area was already gotten by user in [DataTransferPageRest#convertData]
    dataTransferAreaDao.internalUpdate(dbObj)
    return ResponseEntity.ok(ResponseAction(targetType = TargetType.UPDATE).addVariable("data", convertData(id).second))
  }

  private fun isLoggedInUserObserver(
    dto: DataTransferArea,
    user: PFUserDO = ThreadLocalUserContext.getUser()
  ): Boolean {
    return dto.observers?.any { it.id == user.id } ?: false
  }

  /**
   * @return true, if the area isn't a personal box and the user has write access.
   */
  private fun hasEditAccess(dto: DataTransferArea, dbObj: DataTransferAreaDO): Boolean {
    return dto.personalBox != true && dataTransferAreaDao.hasLoggedInUserUpdateAccess(dbObj, dbObj, false)
  }

  private fun convertData(id: Int): Pair<DataTransferAreaDO, DataTransferArea> {
    val dbObj = dataTransferAreaDao.getById(id)
    val dto = DataTransferArea.transformFromDB(dbObj, dataTransferAreaDao, groupService, userService)
    if (hasEditAccess(dto, dbObj)) {
      dto.externalPassword = dbObj.externalPassword
    }
    dto.attachments = attachmentsService.getAttachments(
      dataTransferAreaPagesRest.jcrPath!!,
      id,
      dataTransferAreaPagesRest.attachmentsAccessChecker
    )
    dto.attachments?.forEach {
      it.addExpiryInfo(DataTransferUtils.expiryTimeLeft(it, dbObj.expiryDays))
    }
    dto.internalLink = getUrl(PagesResolver.getDynamicPageUrl(this::class.java, id = id))
    if (!dbObj.accessGroupIds.isNullOrBlank()) {
      // Add all users assigned to the access groups:
      val accessGroupUsers =
        groupService.getGroupUsers(User.toIntArray(dbObj.accessGroupIds)).joinToString { it.displayName }
      dto.accessGroupsAsString += ": $accessGroupUsers"
    }
    dto.userWantsToObserve = isLoggedInUserObserver(dto)
    return Pair(dbObj, dto)
  }
}
