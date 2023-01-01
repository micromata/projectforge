/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.dvelop

import mu.KotlinLogging
import org.projectforge.business.dvelop.MigrateTradingPartners
import org.projectforge.framework.time.DateHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/dvelop")
class DvelopPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var migrateTradingPartners: MigrateTradingPartners

  class DvelopData()

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val layout = UILayout("dvelop.title")
    layout.add(
      MenuItem(
        "downloadBackups",
        title = "Download TradingPartners (Excel)",
        url = "${getRestPath()}/downloadTradingPartners",
        type = MenuItemTargetType.DOWNLOAD
      )
    )
    val data = DvelopData()
    LayoutUtils.process(layout)
    return FormLayoutData(data, layout, createServerData(request))
  }

  @GetMapping("downloadTradingPartners")
  fun downloadBackupScripts(): ResponseEntity<*> {
    log.info("Downloading Trading partners for D-velop import.")
    val filename = ("D-velop-TradingPartners-Import-${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx")
    return RestUtils.downloadFile(filename, migrateTradingPartners.extractTradingPartnersAsExcel())
  }
}
