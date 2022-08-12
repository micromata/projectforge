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

package org.projectforge.rest.calendar

import org.projectforge.Constants
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

/**
 * Page and services for settings for the calendar (independent of filter).
 */
@RestController
@RequestMapping("${Rest.URL}/calendarSettings")
class CalendarSettingsPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var calendarSettingsRest: CalendarSettingsService

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val layout = UILayout("calendar.settings.title")
    val settings = calendarSettingsRest.getSettings()
    layout.add(UIAlert("calendar.settings.intro", color = UIColor.INFO))
    // layout.add(UICheckbox("contrastMode"))
    layout.add(
      UICustomized(UICustomized.TYPE.COLOR_CHOOSER)
        .add("property", CalendarSettings::timesheetsColor.name)
        .add("label", "calendar.settings.timesheetsColor")
    )
    layout.add(
      UICustomized(UICustomized.TYPE.COLOR_CHOOSER).add("property", CalendarSettings::timesheetStatsColor.name)
        .add("label", "calendar.settings.timesheetStatsColor")
    )
    layout.add(
      UICustomized(UICustomized.TYPE.COLOR_CHOOSER).add("property", CalendarSettings::timesheetBreaksColor.name)
        .add("label", "calendar.settings.timesheetBreaksColor")
    )
    layout.add(
      UIButton.createCancelButton(responseAction = ResponseAction("/${Constants.REACT_APP_PATH}calendar"))
    ).add(
      UIButton.createDefaultButton(
        "save",
        title = "save",
        responseAction = ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
          .addVariable("hash", NumberHelper.getSecureRandomAlphanumeric(4)),
      )
    )
    LayoutUtils.process(layout)
    return FormLayoutData(settings, layout, createServerData(request))
  }
}
