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

package org.projectforge.rest

import mu.KotlinLogging
import org.projectforge.business.admin.SystemStatistics
import org.projectforge.business.admin.SystemStatisticsData
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/systemStatistics")
class SystemStatisticPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var accessChecker: AccessChecker

  @Autowired
  private lateinit var systemStatistics: SystemStatistics

  /**
   * Rest service for getting system statistics.
   * @return The system statistics for data base usage as well as for memory usage.
   */
 /* @GetMapping("adminXlsExport")
  fun getSystemStatistics(): ResponseEntity<*> {
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
    log.info("Admin tries to export system statistics.")
    val xls = addressExport.export(list, personalAddressMap)
    if (xls == null || xls.isEmpty()) {
      return ResponseEntity(ResponseData("address.book.hasNoVCards", messageType = MessageType.TOAST, color = UIColor.WARNING), HttpStatus.NOT_FOUND)
    }
    val filename = ("ProjectForge-AddressExport_" + DateHelper.getDateAsFilenameSuffix(Date())
        + ".xls")

    val resource = ByteArrayResource(xls)
    return RestUtils.downloadFile(filename, resource)
  }*/

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val statsData = systemStatistics.getSystemStatistics()
    val layout = UILayout("system.statistics.title")

    statsData.groups.forEach {group ->
      val fieldset = UIFieldset(title = "'${group.capitalize()}")
      statsData.filterEntries(group).forEach {
        fieldset.add(createRow(it.title, it.valueAsString()))
      }
      layout.add(fieldset)
    }
    if (accessChecker.isUserMemberOfAdminGroup(ThreadLocalUserContext.getUser())) {
      layout.add(
        MenuItem(
          "adminXlsExport",
          i18nKey = "menu.systemStatistics.adminExport",
          url = RestResolver.getRestUrl(this::class.java, "exportAdminStats"),
          type = MenuItemTargetType.DOWNLOAD
        )
      )
    }
    LayoutUtils.process(layout)
    layout.postProcessPageMenu()
    return FormLayoutData(statsData, layout, createServerData(request))
  }

  private fun createRow(label: String, value: String): UIRow {
    return UIRow()
      .add(
        UICol(UILength(12, 6, 6, 4, 3))
          .add(UILabel(label))
      )
      .add(
        UICol(UILength(12, 6, 6, 8, 9))
          .add(UILabel("'$value"))
      )
  }
}
