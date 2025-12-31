/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.datatransfer.restPublic

import org.projectforge.common.NumberOfBytes
import org.projectforge.framework.i18n.translate
import org.projectforge.model.rest.RestPaths
import org.projectforge.plugins.datatransfer.DataTransferAreaDO
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.plugins.datatransfer.DataTransferPlugin
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.ServerData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/**
 * For external anonymous usage via token/password.
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/datatransfer")
class DataTransferPublicPageRest : AbstractDynamicPageRest() {
  private lateinit var dataTransferPublicAccessChecker: DataTransferPublicAccessChecker

  @Autowired
  private lateinit var dataTransferPublicServicesRest: DataTransferPublicServicesRest

  @Autowired
  private lateinit var dataTransferPublicSession: DataTransferPublicSession

  @PostConstruct
  private fun postConstruct() {
    dataTransferPublicAccessChecker = DataTransferPublicAccessChecker(dataTransferPublicSession)
  }

  /**
   * The main page. If not logged-in, the login form will be shown, otherwise the file list of the data transfer area.
   */
  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, @RequestParam("id") externalAccessToken: String?): FormLayoutData {
    dataTransferPublicSession.checkLogin(request, accessToken = externalAccessToken)?.let {
      // Already logged-in, accessToken, password and settings of the area are OK.
      val data = dataTransferPublicServicesRest.convert(request, it.first, it.second.userInfo)
      return FormLayoutData(data, getAttachmentLayout(it.first, data), ServerData())
    }

    // User isn't logged-in:
    val dataTransfer = DataTransferPublicArea()
    dataTransfer.areaName = translate("plugins.datatransfer.title.heading")
    dataTransfer.externalAccessToken = externalAccessToken

    return FormLayoutData(dataTransfer, this.getLoginLayout(), ServerData())
  }

  /**
   * User pressed login button on login form.
   */
  @PostMapping("login")
  fun login(
    request: HttpServletRequest,
    response: HttpServletResponse,
    @RequestBody postData: PostData<DataTransferPublicArea>
  )
      : ResponseAction {
    val externalAccessToken = postData.data.externalAccessToken
    val externalPassword = postData.data.externalPassword
    val userInfo = postData.data.userInfo
    val checkAccess = dataTransferPublicSession.login(request, externalAccessToken, externalPassword, userInfo)
    checkAccess.failedAccessMessage?.let {
      return getLoginFailed(response, it)
    }
    val dbo = checkAccess.dataTransferArea!!
    val data = dataTransferPublicServicesRest.convert(request, dbo, userInfo)

    return ResponseAction(targetType = TargetType.UPDATE)
      .addVariable("ui", getAttachmentLayout(dbo, data)) // Show list of attachments.
      .addVariable("data", data)
  }

  @GetMapping("logout")
  fun logout(
    request: HttpServletRequest,
    response: HttpServletResponse,
    @RequestParam("accessToken") externalAccessToken: String?
  ): ResponseAction {
    dataTransferPublicSession.logout(request)
    return getLoginFailed(response, translate("logout.successful"))
    //return ResponseAction("/${RestResolver.REACT_PUBLIC_PATH}/datatransfer/dynamic/$externalAccessToken")
  }

  private fun getAttachmentLayout(dbObj: DataTransferAreaDO, dataTransfer: DataTransferPublicArea): UILayout {
    val fieldSet = UIFieldset(12, title = "'${dataTransfer.areaName}")
    fieldSet.add(
      UIFieldset(title = "attachment.list")
        .add(
          UIAttachmentList(
            DataTransferPlugin.ID,
            dataTransfer.id,
            serviceBaseUrl = PagesResolver.getDynamicPageUrl(
              DataTransferPublicAttachmentPageRest::class.java,
              absolute = true,
              trailingSlash = false
            ),
            restBaseUrl = "/${RestPaths.REST_PUBLIC}/datatransfer",
            downloadOnRowClick = false,
            uploadDisabled = dataTransfer.externalUploadEnabled != true,
            showExpiryInfo = true,
            maxSizeInKB = DataTransferAreaDao.getMaxUploadFileSizeKB(dbObj),
            showUserInfo = false,
          )
        )
    )
    val layout = UILayout("plugins.datatransfer.title.heading")
      .add(fieldSet)
    if ((dataTransfer.attachmentsSize ?: 0) in 1..NumberOfBytes.GIGA_BYTES) {
      // Download all not for attachments with size of more than 1 GB in total.
      fieldSet.add(
        UIButton.createDownloadButton(
          id = "downloadAll",
          title = "plugins.datatransfer.button.downloadAll",
          tooltip = "plugins.datatransfer.button.downloadAll.info",
          responseAction = ResponseAction(
            RestResolver.getPublicRestUrl(
              this.javaClass,
              "downloadAll/datatransfer/${dataTransfer.id}"
            ), targetType = TargetType.DOWNLOAD
          ),
          default = true
        )
      )
    }
    fieldSet.add(
      UIButton.createDangerButton(
        "logout",
        title = "menu.logout",
        responseAction = ResponseAction(
          RestResolver.getPublicRestUrl(
            this.javaClass,
            "logout",
            params = mapOf("accessToken" to dataTransfer.externalAccessToken)
          ), targetType = TargetType.GET
        ),
      )
    )
    LayoutUtils.process(layout)
    return layout
  }

  private fun getLoginFailed(response: HttpServletResponse, msg: String): ResponseAction {
    response.status = 400
    return ResponseAction(targetType = TargetType.UPDATE)
      .addVariable("ui", getLoginLayout(msg))
  }

  private fun getLoginLayout(alertMessage: String? = null): UILayout {
    val responseAction =
      ResponseAction(RestResolver.getRestUrl(this::class.java, "login"), targetType = TargetType.POST)

    val formCol = UICol(length = UILength(12, md = 6, lg = 4), offset = UILength(0, md = 3, lg = 4))

    if (alertMessage != null) {
      formCol.add(
        UIAlert("'$alertMessage", color = UIColor.DANGER, icon = UIIconType.USER_LOCK)
      )
    }

    formCol
      .add(
        UIInput(
          "externalPassword",
          required = true,
          label = "password",
          focus = true,
          dataType = UIDataType.PASSWORD,
          autoComplete = UIInput.AutoCompleteType.CURRENT_PASSWORD
        )
      )
      .add(
        UIInput(
          "userInfo",
          label = "plugins.datatransfer.external.userInfo",
          tooltip = "plugins.datatransfer.external.userInfo.info",
          maxLength = 255
        )
      )
      .add(
        UIButton.createDefaultButton(
          "login",
          responseAction = responseAction,
        )
      )

    val layout = UILayout("plugins.datatransfer.title.heading")
      .add(
        UIRow()
          .add(formCol)
      )
    LayoutUtils.process(layout)
    return layout
  }
}
