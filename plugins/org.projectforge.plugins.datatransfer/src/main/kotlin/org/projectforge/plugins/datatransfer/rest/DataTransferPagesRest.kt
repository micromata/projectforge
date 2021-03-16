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

package org.projectforge.plugins.DataTransferFile.rest

import org.projectforge.framework.i18n.translate
import org.projectforge.plugins.datatransfer.DataTransferDO
import org.projectforge.plugins.datatransfer.DataTransferDao
import org.projectforge.plugins.datatransfer.rest.DataTransfer
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@RestController
@RequestMapping("${Rest.URL}/datatransfer")
class DataTransferPagesRest() : AbstractDTOPagesRest<DataTransferDO, DataTransfer, DataTransferDao>(
  DataTransferDao::class.java,
  "plugins.datatransfer.title"
) {

  override fun transformForDB(dto: DataTransfer): DataTransferDO {
    val obj = DataTransferDO()
    dto.copyTo(obj)
    return obj
  }

  override fun transformFromDB(obj: DataTransferDO, editMode: Boolean): DataTransfer {
    val dto = DataTransfer()
    dto.copyFrom(obj)
    dto.externalLinkBaseUrl = baseDao.getExternalBaseLinkUrl()
    return dto
  }

  /**
   * Initializes new DataTransferFiles for adding.
   */
  override fun newBaseDO(request: HttpServletRequest?): DataTransferDO {
    return baseDao.createInitializedFile()
  }

  @PostMapping("renewAccessToken")
  fun renewAccessToken(@Valid @RequestBody postData: PostData<DataTransfer>): ResponseAction {
    val file = postData.data
    file.externalAccessToken = DataTransferDao.generateExternalAccessToken()
    return ResponseAction(targetType = TargetType.UPDATE)
      .addVariable("data", file)
  }

  @PostMapping("renewPassword")
  fun renewPassword(@Valid @RequestBody postData: PostData<DataTransfer>): ResponseAction {
    val file = postData.data
    file.externalPassword = DataTransferDao.generateExternalPassword()
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
          .add(lc, "created", "lastUpdate", "filename", "owner", "ownerGroup", "validUntil", "comment")
      )
    return LayoutUtils.processListPage(layout, this)
  }

  /**
   * LAYOUT Edit page
   */
  override fun createEditLayout(dto: DataTransfer, userAccess: UILayout.UserAccess): UILayout {
    val layout = super.createEditLayout(dto, userAccess)
      .add(lc, "areaName", "externalDownloadEnabled", "externalUploadEnabled", "description", "externalPassword")
      .add(UIReadOnlyField("accessFailedCounter", lc))
      .add(
        UIRow()
          .add(
            UICol(10)
              .add(
                UIReadOnlyField(
                  "externalLink",
                  lc,
                  label = "plugins.datatransfer.external.link",
                  canCopy = true
                )
              )
          )
          .add(
            UICol(2)
              .add(
                UIButton(
                  "accessToken-renew",
                  title = translate("plugins.datatransfer.external.link.renew"),
                  tooltip = "plugins.datatransfer.external.link.renew.info",
                  color = UIColor.DANGER,
                  responseAction = ResponseAction("/rs/datatransfer/renewAccessToken", targetType = TargetType.POST)
                )
              )
          )
      )

    return LayoutUtils.processEditPage(layout, dto, this)
  }
}
