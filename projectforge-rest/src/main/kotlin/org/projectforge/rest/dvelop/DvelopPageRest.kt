/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.dvelop.ExtractPFTradingPartners
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
import jakarta.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/dvelop")
class DvelopPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var extractPFTradingPartners: ExtractPFTradingPartners

  @Autowired
  private lateinit var tradingPartnerService: TradingPartnerService

  class DvelopData()

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val layout = UILayout("dvelop.title")
    layout.add(
      MenuItem(
        "syncTradingPartners",
        title = "Synchronize TradingPartners",
        url = "${getRestPath()}/synchronizeTradingPartners",
        type = MenuItemTargetType.DOWNLOAD
      )
    )
    layout.add(
      MenuItem(
        "downloadTradingPartners",
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
  fun downloadTradingPartners(): ResponseEntity<*> {
    log.info("Downloading Trading partners for D-velop import.")
    val filename = ("D-velop-TradingPartners-Import-${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx")
    return RestUtils.downloadFile(
      filename,
      extractPFTradingPartners.extractTradingPartnersAsExcel(tradingPartnerService.getList())
    )
  }

  @GetMapping("synchronizeTradingPartners")
  fun synchronizeTradingPartners(): ResponseAction {
    log.info("Synchronizing Trading partners for D-velop import.")
    val localPartners = extractPFTradingPartners.extractTradingPartners()
    val remotePartners = tradingPartnerService.getList()
    var insertedCounter = 0
    var failedCounter = 0
    var ignoredCounter = 0
    var totalCounter = 0
    var modifiedCounter = 0
    var unmodifiedCounter = 0
    localPartners.forEach { localPartner ->
      ++totalCounter
      if (localPartner.importCode == null) {
        ++ignoredCounter
      } else {
        val remotePartner = remotePartners.find { it.number == localPartner.number }
        if (remotePartner != null) {
          if (tradingPartnerService.update(localPartner, remotePartner)) {
            ++modifiedCounter
          } else {
            ++unmodifiedCounter
          }
        } else if (tradingPartnerService.create(localPartner)) {
          ++insertedCounter
        } else {
          ++failedCounter
        }
      }
    }
    val msg =
      "Total=$totalCounter: ${insertedCounter + modifiedCounter} TradingPartners sent to D.velop ($insertedCounter inserted, $modifiedCounter modified, $failedCounter entries failed, $ignoredCounter ignored/no importCode, $unmodifiedCounter unmodified)."

    log.info(msg)
    return UIToast.createToast(
      msg, color = UIColor.SUCCESS
    )
  }
}
