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
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.model.rest.RestPaths
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaPagesRest
import org.projectforge.rest.config.Rest
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
class DataTransferPublicPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  private lateinit var attachmentsAccessChecker: DataTransferPublicAccessChecker

  @Autowired
  private lateinit var dataTransferAreaDao: DataTransferAreaDao

  @Autowired
  private lateinit var dataTransferAreaPagesRest: DataTransferAreaPagesRest

  @PostConstruct
  private fun postConstruct() {
    attachmentsAccessChecker = DataTransferPublicAccessChecker(dataTransferAreaDao)
  }

  @PostMapping("login")
  fun login(
    request: HttpServletRequest,
    response: HttpServletResponse,
    @RequestBody postData: PostData<DataTransferPublicArea>
  )
      : ResponseAction {
    val externalAccessToken = postData.data.externalAccessToken
    val externalPassword = postData.data.externalPassword
    val checkAccess =
      attachmentsAccessChecker.checkExternalAccess(dataTransferAreaDao, request, externalAccessToken, externalPassword)
    checkAccess.second?.let {
      return getLoginFailed(response, it)
    }
    val data = DataTransferPublicArea()
    data.copyFrom(checkAccess.first!!)
    data.attachments = attachmentsAccessChecker.filterAttachments(
      request, data.externalDownloadEnabled,
      attachmentsService.getAttachments(
        dataTransferAreaPagesRest.jcrPath!!,
        data.id!!,
        attachmentsAccessChecker
      )
    )

    return ResponseAction(targetType = TargetType.UPDATE)
      .addVariable("ui", getAttachmentLayout(data))
      .addVariable("data", data)
  }

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, @RequestParam("id") externalAccessToken: String?): FormLayoutData {
    val dataTransfer = DataTransferPublicArea()
    dataTransfer.areaName = translate("plugins.datatransfer.title.heading")
    dataTransfer.externalAccessToken = externalAccessToken

    return FormLayoutData(dataTransfer, this.getLayout(), ServerData())
  }

  private fun getAttachmentLayout(dataTransfer: DataTransferPublicArea): UILayout {
    val fieldSet = UIFieldset(12, title = "'${dataTransfer.areaName}")
    fieldSet.add(
      UIFieldset(title = "attachment.list")
        .add(
          UIAttachmentList(
            "datatransfer",
            dataTransfer.id,
            //serviceBaseUrl = "/${RestResolver.REACT_PUBLIC_PATH}/datatransferattachment/dynamic",
            restBaseUrl = "/${RestPaths.REST_PUBLIC}/datatransfer",
            accessString = "${dataTransfer.externalAccessToken}|${dataTransfer.externalPassword}",
            downloadOnRowClick = true,
            uploadDisabled = dataTransfer.externalUploadEnabled != true
          )
        )
    )
    val layout = UILayout("plugins.datatransfer.title.heading")
      .add(fieldSet)
    LayoutUtils.process(layout)
    return layout
  }

  private fun getLoginFailed(response: HttpServletResponse, msg: String): ResponseAction {
    response.status = 400
    return ResponseAction(targetType = TargetType.UPDATE)
      .addVariable("ui", getLayout(msg))
  }

  private fun getLayout(alertMessage: String? = null): UILayout {
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
