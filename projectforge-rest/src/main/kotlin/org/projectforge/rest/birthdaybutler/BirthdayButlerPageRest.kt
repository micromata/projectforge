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

package org.projectforge.rest.birthdaybutler

import mu.KotlinLogging
import org.projectforge.birthdaybutler.BirthdayButlerConfiguration
import org.projectforge.birthdaybutler.BirthdayButlerService
import org.projectforge.business.scripting.I18n
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.Month
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/birthdayButler")
class BirthdayButlerPageRest : AbstractDynamicPageRest() {

  @Autowired
  private lateinit var accessChecker: AccessChecker

  @Autowired
  private lateinit var birthdayButlerConfiguration: BirthdayButlerConfiguration

  @Autowired
  private lateinit var birthdayButlerService: BirthdayButlerService

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {

    accessChecker.checkIsLoggedInUserMemberOfGroup(ProjectForgeGroup.ORGA_TEAM, ProjectForgeGroup.HR_GROUP)
    val layout = UILayout(translate("menu.birthdayButler"))

    val values = ArrayList<UISelectValue<Int>>()
    Month.values().forEachIndexed { index, month -> values.add(UISelectValue(index + 1, birthdayButlerService.translateMonth(month))) }

    layout.add(UISelect("month", required = true, values = values, label = I18n.getString("calendar.month")))
    layout.addAction(
      UIButton.createDefaultButton(
        id = "download_button",
        title = I18n.getString("download"),
        responseAction = ResponseAction(
          RestResolver.getRestUrl(
            this::class.java,
            "downloadBirthdayList"
          ), targetType = TargetType.POST
        )
      )
    )
    LayoutUtils.process(layout)
    val data = BirthdayButlerData()
    return FormLayoutData(data, layout, createServerData(request))
  }

  @PostMapping("downloadBirthdayList")
  fun downloadBirthdayList(
    request: HttpServletRequest,
    @RequestBody postData: PostData<BirthdayButlerData>
  ): ResponseEntity<*> {
    accessChecker.checkIsLoggedInUserMemberOfGroup(ProjectForgeGroup.ORGA_TEAM, ProjectForgeGroup.HR_GROUP)
    validateCsrfToken(request, postData)?.let { return it }
    val monthValue = postData.data.month
    val month = if (monthValue in 1..12) {
      Month.of(monthValue)
    } else {
      LocalDateTime.now().month
    }
    val response = birthdayButlerService.createWord(month)
    val wordDocument = response.wordDocument
    if (wordDocument != null) {
      log.info { "Birthday list for month ${postData.data.month} created" }
      return RestUtils.downloadFile(birthdayButlerService.createFilename(month), wordDocument)
    }
    val errorMessage = response.errorMessage
    if (errorMessage != null) {
      return ValidationError.createResponseEntity(errorMessage)
    }

    log.info { "No user with birthday in selected month" }
    return ValidationError.createResponseEntity("birthdayList.month.response.noEntry")
  }
}
