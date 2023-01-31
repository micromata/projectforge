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

package org.projectforge.rest.sipgate

import mu.KotlinLogging
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressFilter
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/sipgate")
class SipgatePageRest : AbstractDynamicPageRest() {
  class SipgateData

  @Autowired
  private lateinit var addressDao: AddressDao

  @Autowired
  private lateinit var sipgateContactService: SipgateContactService

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val layout = UILayout("sipgate.title")
    layout.add(
      MenuItem(
        "syncAddresses",
        title = "Synchronize Addresses",
        url = "${getRestPath()}/synchronizeAddresses",
        type = MenuItemTargetType.DOWNLOAD
      )
    )
    val data = SipgateData()
    LayoutUtils.process(layout)
    return FormLayoutData(data, layout, createServerData(request))
  }

  @GetMapping("synchronizeAddresses")
  fun synchronizeAddresses(): ResponseAction {
    log.info("Synchronizing addresses for Sipgate.")
    var insertedCounter = 0
    var failedCounter = 0
    var ignoredCounter = 0
    var totalCounter = 0
    var modifiedCounter = 0
    var unmodifiedCounter = 0
    val remoteContacts = sipgateContactService.getList().filter { it.scope == SipgateContact.Scope.SHARED }
    val filter = AddressFilter()
    filter.isActive = true
    val addressList = addressDao.getList(filter)
    addressList.forEach { address ->
      ++totalCounter
      val remoteContact = SipgateContactSyncService.findBestMatch(remoteContacts, address)
      val contact = SipgateContactSyncService.from(address)
      if (remoteContact != null) {
        remoteContact.id?.let { id ->
          if (sipgateContactService.update(id, contact)) {
            ++modifiedCounter
          } else {
            ++unmodifiedCounter
          }
        } ?: ++ignoredCounter // Shouldn't occur.
      } else if (sipgateContactService.create(contact)) {
        ++insertedCounter
      } else {
        ++failedCounter
      }
    }
    val msg =
      "Total=$totalCounter: ${insertedCounter + modifiedCounter} addresses sent to Sipgate ($insertedCounter inserted, $modifiedCounter modified, $failedCounter entries failed, $ignoredCounter ignored/no importCode, $unmodifiedCounter unmodified)."

    log.info(msg)
    return UIToast.createToast(
      msg, color = UIColor.SUCCESS
    )
  }
}
