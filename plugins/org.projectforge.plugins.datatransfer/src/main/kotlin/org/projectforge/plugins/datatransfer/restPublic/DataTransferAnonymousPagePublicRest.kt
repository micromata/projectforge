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

package org.projectforge.plugins.datatransfer.restPublic

import mu.KotlinLogging
import org.projectforge.business.login.LoginProtection
import org.projectforge.business.login.LoginResultStatus
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.AttachmentsAccessChecker
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaPagesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.ServerData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val log = KotlinLogging.logger {}

/**
 * For external anonymous usage via token/password.
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/datatransfer")
class DataTransferPagePublicRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  private lateinit var attachmentsAccessChecker: AttachmentsAccessChecker

  @Autowired
  private lateinit var dataTransferAreaDao: DataTransferAreaDao

  @Autowired
  private lateinit var dataTransferAreaPagesRest: DataTransferAreaPagesRest

  @PostConstruct
  private fun postConstruct() {
    attachmentsAccessChecker =
      DataTransferExternalAnonymousAccessChecker(
        dataTransferAreaDao.maxFileSize.toBytes(),
        DataTransferAreaDao.MAX_FILE_SIZE_SPRING_PROPERTY
      )
  }

  @PostMapping("login")
  fun login(
    request: HttpServletRequest,
    response: HttpServletResponse,
    @RequestBody postData: PostData<DataTransferAnonymousArea>
  )
      : ResponseAction {
    val externalAccessToken = postData.data.externalAccessToken
    val externalPassword = postData.data.externalPassword
    if (externalAccessToken == null || externalPassword == null) {
      return getLoginFailed(response, LoginResultStatus.FAILED.localizedMessage)
    }
    val loginProtection = LoginProtection.instance()
    val clientIpAddress = RestUtils.getClientIp(request)
    val offset = loginProtection.getFailedLoginTimeOffsetIfExists(externalAccessToken, clientIpAddress)
    if (offset > 0) {
      // Time offset still exists. Ignore login try.
      val seconds = (offset / 1000).toString()
      log.warn("The account for '${externalAccessToken}', ip=$clientIpAddress is locked for $seconds seconds due to failed login attempts. Please try again later.")
      val numberOfFailedAttempts = loginProtection.getNumberOfFailedLoginAttempts(externalAccessToken, clientIpAddress)
      val loginResultStatus = LoginResultStatus.LOGIN_TIME_OFFSET
      loginResultStatus.setMsgParams(
        seconds,
        numberOfFailedAttempts.toString()
      )
      return getLoginFailed(response, loginResultStatus.localizedMessage)
    }

    val dbo = dataTransferAreaDao.getAnonymousArea(externalAccessToken)
    if (dbo == null) {
      log.warn { "Data transfer area with externalAccessToken '$externalAccessToken' not found. Requested by ip=$clientIpAddress." }
      loginProtection.incrementFailedLoginTimeOffset(externalAccessToken, clientIpAddress)
      return getLoginFailed(response, LoginResultStatus.FAILED.localizedMessage)
    }
    if (dbo.externalPassword != externalPassword) {
      log.warn { "Data transfer area with externalAccessToken '$externalAccessToken' doesn't match given password. Requested by ip=$clientIpAddress." }
      loginProtection.incrementFailedLoginTimeOffset(externalAccessToken, clientIpAddress)
      return getLoginFailed(response, LoginResultStatus.FAILED.localizedMessage)
    }

    // Successfully logged in:
    loginProtection.clearLoginTimeOffset(externalAccessToken, null, clientIpAddress)

    val data = DataTransferAnonymousArea()
    data.copyFrom(dbo)
    data.attachments =
      attachmentsService.getAttachments(
        dataTransferAreaPagesRest.jcrPath!!,
        data.id!!,
        attachmentsAccessChecker
      )
    return ResponseAction(targetType = TargetType.UPDATE)
      .addVariable("ui", getAttachmentLayout(data))
      .addVariable("data", data)
  }

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, @RequestParam("id") externalAccessToken: String?): FormLayoutData {
    val dataTransfer = DataTransferAnonymousArea()
    dataTransfer.areaName = translate("plugins.datatransfer.title.heading")
    dataTransfer.externalAccessToken = externalAccessToken

    return FormLayoutData(dataTransfer, this.getLoginLayout(), ServerData())
  }

  private fun getAttachmentLayout(dataTransfer: DataTransferAnonymousArea): UILayout {
    val fieldSet = UIFieldset(12, title = "'${dataTransfer.areaName}")
    fieldSet.add(
      UIFieldset(title = "attachment.list")
        .add(UIAttachmentList("datatransfer", 28738162))
    )
    val layout = UILayout("plugins.datatransfer.title.heading")
      .add(fieldSet)
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
          "externalAccessToken",
          required = true,
          label = "plugins.datatransfer.external.accessToken",
          autoComplete = UIInput.AutoCompleteType.USERNAME
        )
      )
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
        UIButton(
          "login",
          translate("login"),
          UIColor.SUCCESS,
          responseAction = responseAction,
          default = true
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
