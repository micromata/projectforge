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

package org.projectforge.rest.admin

import mu.KotlinLogging
import org.projectforge.common.logging.LogFilter
import org.projectforge.common.logging.LoggerMemoryAppender
import org.projectforge.common.logging.LoggingEventData
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/logViewer")
class LogViewerPageRest : AbstractDynamicPageRest() {
  class Data(val logEntries: List<LogViewerEvent>)

  @Autowired
  lateinit var accessChecker: AccessChecker

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
    val lc = LayoutContext(LogViewerEvent::class.java)

    val layout = UILayout("system.admin.logViewer.title")
      .add(UITable("logEntries")
        .add(lc, "timestamp", "level", "message", "user", "userAgent", "stackTrace"))
    /*layout.add(MenuItem("EDIT",
            i18nKey = "address.title.edit",
            url = PagesResolver.getEditPageUrl(AddressPagesRest::class.java, address.id),
            type = MenuItemTargetType.REDIRECT))*/
    LayoutUtils.process(layout)
    layout.postProcessPageMenu()

    val logEntries = LoggerMemoryAppender.getInstance().query(LogFilter()).map { LogViewerEvent(it) }
    return FormLayoutData(Data(logEntries), layout, createServerData(request))
  }
}
