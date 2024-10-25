/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/systemStatistics")
class SystemStatisticPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var systemStatistics: SystemStatistics

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val statsData = systemStatistics.getSystemStatistics()
    val layout = UILayout("system.statistics.title")

    statsData.groups.forEach { group ->
      val fieldset = UIFieldset(title = "'${group.replaceFirstChar { it.uppercase() }}")
      statsData.filterEntries(group).forEach {
        fieldset.add(createRow(it.title, it.valueAsString()))
      }
      layout.add(fieldset)
    }
    LayoutUtils.process(layout)
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
