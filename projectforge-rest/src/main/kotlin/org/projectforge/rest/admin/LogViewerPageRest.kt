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

import org.projectforge.business.user.service.UserPrefService
import org.projectforge.common.logging.LoggerMemoryAppender
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

// private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/logViewer")
class LogViewerPageRest : AbstractDynamicPageRest() {
  @Autowired
  lateinit var accessChecker: AccessChecker

  @Autowired
  private lateinit var userPrefService: UserPrefService

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
    val lc = LayoutContext(LogViewerEvent::class.java)
    val filterLc = LayoutContext(LogViewFilter::class.java)
    val logViewFilter = getUserPref()

    val layout = UILayout("system.admin.logViewer.title")
      .add(
        UIFieldset()
          .add(
            UIRow()
              .add(
                UICol(UILength(md = 8))
                  .add(filterLc, "search")
              )
              .add(
                UICol(UILength(md = 2))
                  .add(filterLc, "threshold")
              )
              .add(
                UICol(UILength(md = 2))
                  .add(
                    filterLc, "autoRefresh"
                  )
              )
          )
      )
      .add(
        UIButton(
          "refresh",
          title = translate("refresh"),
          color = UIColor.SUCCESS,
          default = true,
          responseAction = ResponseAction(
            RestResolver.getRestUrl(this::class.java, "search"),
            targetType = TargetType.POST
          )
        )
      )
      .add(
        UITable(
          "logEntries",
          refreshUrl = "/rs/logViewer/refresh",
          refreshIntervalSeconds = 5,
          autoRefreshFlag = "autoRefresh"
        )
          .add(lc, "timestamp", "level", "user", "message", "userAgent", "stackTrace", sortable = false)
      )
    layout.watchFields.addAll(arrayOf("threshold", "search", "refreshInterval"))
    LayoutUtils.process(layout)
    layout.postProcessPageMenu()

    val logEntries = queryList(logViewFilter)
    return FormLayoutData(
      logViewFilter,
      layout,
      createServerData(request),
      variables = mutableMapOf("logEntries" to logEntries)
    )
  }

  @PostMapping("refresh")
  fun refresh(
    @RequestBody postData: PostData<LogViewFilter>
  )
      : List<LogViewerEvent> {
    return getList(postData)
  }

  @PostMapping(RestPaths.WATCH_FIELDS)
  fun watchFields(@Valid @RequestBody postData: PostData<LogViewFilter>): ResponseAction {
    return search(postData)
  }


  @PostMapping("search")
  fun search(
    @RequestBody postData: PostData<LogViewFilter>
  )
      : ResponseAction {
    val variables = mapOf("logEntries" to getList(postData))
    return ResponseAction(targetType = TargetType.UPDATE, merge = true)
      .addVariable("variables", variables)
  }

  private fun getList(postData: PostData<LogViewFilter>): List<LogViewerEvent> {
    val filter = postData.data
    val userPref = getUserPref()
    userPref.search = filter.search
    userPref.threshold = filter.threshold
    return queryList(filter)
  }

  private fun queryList(filter: LogViewFilter): List<LogViewerEvent> {
    return LoggerMemoryAppender.getInstance().query(filter.logFilter).map { LogViewerEvent(it) }
  }

  private fun getUserPref(): LogViewFilter {
    return userPrefService.ensureEntry("logging", "logViewFilter", LogViewFilter())
  }
}
