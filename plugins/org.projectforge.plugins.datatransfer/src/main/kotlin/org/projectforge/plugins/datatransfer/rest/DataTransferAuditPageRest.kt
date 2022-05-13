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

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.plugins.datatransfer.DataTransferAuditDO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.LayoutUtils
import org.projectforge.ui.UILayout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

/**
 * Page of data transfer area with attachments list (including upload/download and editing).
 */
@RestController
@RequestMapping("${Rest.URL}/datatransferaudit")
class DataTransferAuditPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var dataTransferAreaPagesRest: DataTransferAreaPagesRest

  @Autowired
  private lateinit var dataTransferAreaDao: DataTransferAreaDao

  class AreaAuditData(
    var area: DataTransferArea? = null,
    val auditEntries: List<DataTransferAuditDO>? = null,
    val downloadAditEntries: List<DataTransferAuditDO>? = null,
  )

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, @RequestParam("id") idString: String?): FormLayoutData {
    var id = NumberHelper.parseInteger(idString)
    if (id == -1) {
      // personal box of logged-in user is requested:
      id = dataTransferAreaDao.ensurePersonalBox(ThreadLocalUserContext.getUserId())?.id
    }
    id ?: throw IllegalAccessException("Parameter id not an int or no personal box found.")
    val areaDO = dataTransferAreaDao.getById(id)
    val area = DataTransferArea()
    area.copyFrom(areaDO)
    val layout = UILayout("plugins.datatransfer.audit")

    LayoutUtils.process(layout)
    layout.postProcessPageMenu()
    val formData = AreaAuditData(area)
    return FormLayoutData(formData, layout, createServerData(request))
  }
}
