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

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, @RequestParam("id") idString: String?): FormLayoutData {
    val id = NumberHelper.parseInteger(idString) ?: throw IllegalAccessException("Parameter id not an int.")
    val dataTransferDO = dataTransferAreaDao.getById(id)
    val dataTransfer = DataTransferArea()
    dataTransfer.id = dataTransferDO.id
    dataTransfer.areaName = dataTransferDO.areaName
    dataTransfer.description = dataTransferDO.description
    dataTransfer.attachments = attachmentsService.getAttachments(
      dataTransferAreaPagesRest.jcrPath!!,
      id,
      dataTransferAreaPagesRest.attachmentsAccessChecker
    )
    dataTransfer.externalLinkBaseUrl = dataTransferAreaDao.getExternalBaseLinkUrl()
    val layout = UILayout("plugins.datatransfer.title.heading")
    val fieldSet = UIFieldset(12, title = "'${dataTransfer.areaName}")
    fieldSet.add(
      UIFieldset(title = "attachment.list")
        .add(UIAttachmentList("datatransfer", id))
    )
    layout.add(fieldSet)

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

    if (dataTransferAreaDao.hasLoggedInUserUpdateAccess(dataTransferDO, dataTransferDO, false)) {
      layout.add(
        MenuItem(
          "edit",
          i18nKey = "plugins.datatransfer.title.edit",
          url = PagesResolver.getEditPageUrl(DataTransferAreaPagesRest::class.java, dataTransfer.id),
          type = MenuItemTargetType.REDIRECT
        )
      )
    }

    LayoutUtils.process(layout)
    layout.postProcessPageMenu()
    return FormLayoutData(dataTransfer, layout, createServerData(request))
  }
}
