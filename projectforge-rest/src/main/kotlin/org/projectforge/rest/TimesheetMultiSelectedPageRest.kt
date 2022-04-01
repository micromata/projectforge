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

package org.projectforge.rest

import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.common.logging.LogEventLoggerNameMatcher
import org.projectforge.common.logging.LogSubscription
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UICustomized
import org.projectforge.ui.UILayout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable
import javax.servlet.http.HttpServletRequest

/**
 * Mass update after selection.
 */
@RestController
@RequestMapping("${Rest.URL}/timesheet${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class TimesheetMultiSelectedPageRest : AbstractMultiSelectedPage() {

  @Autowired
  private lateinit var timesheetDao: TimesheetDao


  override fun getTitleKey(): String {
    return "timesheet.multiselected.title"
  }

  override val listPageUrl: String = "/${MenuItemDefId.OUTGOING_INVOICE_LIST.url}"

  override val pagesRestClass: Class<out AbstractPagesRest<*, *, *>>
    get() = TimesheetPagesRest::class.java

  override fun fillForm(
    request: HttpServletRequest,
    layout: UILayout,
    massUpdateData: MutableMap<String, MassUpdateParameter>
  ) {
    val lc = LayoutContext(TimesheetDO::class.java)
    //createAndAddFields(lc, massUpdateData, layout, "task")
    /*if (Configuration.instance.isCostConfigured) {
      createAndAddFields(lc, massUpdateData, layout, "kost2")
    }*/
    layout.add(UICustomized("timesheet.edit.taskAndKost2", values = mutableMapOf("id" to "kost2")))
    if (!timesheetDao.getTags().isNullOrEmpty()) {
      createAndAddFields(lc, massUpdateData, layout, "tag")
    }
    createAndAddFields(
      lc,
      massUpdateData,
      layout,
      "location",
      "reference",
      minLengthOfTextArea = 1000,
    )
  }

  override fun proceedMassUpdate(
    request: HttpServletRequest,
    params: Map<String, MassUpdateParameter>,
    selectedIds: Collection<Serializable>
  ): ResponseEntity<*> {
    val timesheets = timesheetDao.getListByIds(selectedIds)
    if (timesheets.isNullOrEmpty()) {
      return showNoEntriesValidationError()
    }
    timesheets.forEach { timesheet ->
      //processTextParameter(timesheet, "bemerkung", params, append = true)
      timesheetDao.update(timesheet)
    }
    return showToast(timesheets.size)
  }

  override fun ensureUserLogSubscription(): LogSubscription {
    val username = ThreadLocalUserContext.getUser().username ?: throw InternalError("User not given")
    val displayTitle = translate("fibu.timesheet.multiselected.title")
    return LogSubscription.ensureSubscription(
      title = "Timesheets",
      displayTitle = displayTitle,
      user = username,
      create = { title, user ->
        LogSubscription(
          title,
          user,
          LogEventLoggerNameMatcher(
            "de.micromata.fibu.TimesheetDao",
            "org.projectforge.framework.persistence.api.BaseDaoSupport|TimesheetDO"
          ),
          maxSize = 10000,
          displayTitle = displayTitle
        )
      })
  }
}
