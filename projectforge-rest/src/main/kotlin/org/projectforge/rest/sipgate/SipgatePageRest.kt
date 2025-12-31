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

package org.projectforge.rest.sipgate

import mu.KotlinLogging
import org.projectforge.framework.access.AccessChecker
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*
import jakarta.servlet.http.HttpServletRequest
import kotlin.concurrent.thread

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/sipgate")
class SipgatePageRest : AbstractDynamicPageRest() {
  class SipgateData

  @Autowired
  private lateinit var accessChecker: AccessChecker

  @Autowired
  private lateinit var sipgateContactSyncService: SipgateContactSyncService

  @Autowired
  private lateinit var sipateSyncService: SipgateSyncService

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val layout = UILayout("sipgate.title")
    layout.add(
      MenuItem(
        "downloadConfiguration",
        title = "Download Sipgate-configuration",
        url = "${getRestPath()}/downloadConfiguration",
        type = MenuItemTargetType.DOWNLOAD
      )
    )
    layout.add(
      MenuItem(
        "sync",
        title = "Synchronize (config and addresses)",
        tooltip = "Synchronizes contacts of Sipgate with local addresses and gets remote config (user, devices and numbers etc.).",
        url = "${getRestPath()}/synchronize",
        type = MenuItemTargetType.DOWNLOAD
      )
    )
    layout.add(
      MenuItem(
        "resetContacts",
        title = "Reset Sipgate contacts",
        tooltip = "Sync and resets Sipgate contacts. May be used for re-organizing contacts in Sipgate having duplicated number entries....",
        url = "${getRestPath()}/resetContacts",
        type = MenuItemTargetType.DOWNLOAD
      )
    )
    val data = SipgateData()
    LayoutUtils.process(layout)
    return FormLayoutData(data, layout, createServerData(request))
  }

  @GetMapping("downloadConfiguration")
  fun downloadConfiguration(): ResponseEntity<ByteArrayResource> {
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
    log.info("Downloading configuration (users, devices etc.) of Sipgate.")
    return SipgateExcelExporter.download(sipateSyncService.readStorage())
  }

  @GetMapping("synchronize")
  fun synchronize(): ResponseAction {
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
    thread(start = true) {
      sipateSyncService.readStorage(true)
    }
    log.info("Synchronizing addresses for Sipgate.")
    val ctx = sipgateContactSyncService.sync()
    val msg =
      "Synchronizing result: local addresses: [${ctx.localCounter}], remote contacts: [${ctx.remoteCounter}]."
    log.info(msg)
    return UIToast.createToast(
      msg, color = UIColor.SUCCESS
    )
  }

  @GetMapping("resetContacts")
  fun resetContacts(): ResponseAction {
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
    log.info("Synchronizing addresses for Sipgate.")
    val ctx = sipgateContactSyncService.sync(resetContacts = true)
    val msg =
      "Synchronizing result: local addresses: [${ctx.localCounter}], remote contacts: [${ctx.remoteCounter}]."
    log.info(msg)
    return UIToast.createToast(
      msg, color = UIColor.SUCCESS
    )
  }
}
