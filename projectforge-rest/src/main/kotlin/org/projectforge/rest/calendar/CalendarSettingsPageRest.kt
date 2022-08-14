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
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

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
    // layout.add(UICheckbox("contrastMode"))
    layout.add(
      UIFieldset(12, "calendar.settings.colors")
        .add(UIAlert("calendar.settings.intro", color = UIColor.INFO))
        .add(
          UICustomized(UICustomized.TYPE.COLOR_CHOOSER)
            .add("id", CalendarSettings::timesheetsColor.name)
            .add("label", translate("calendar.settings.colors.timesheets"))
        )
        .add(
          UICustomized(UICustomized.TYPE.COLOR_CHOOSER).add("id", CalendarSettings::timesheetStatsColor.name)
            .add("label", translate("calendar.settings.colors.timesheetStats"))
        )
        .add(
          UICustomized(UICustomized.TYPE.COLOR_CHOOSER).add("id", CalendarSettings::timesheetBreaksColor.name)
            .add("label", translate("calendar.settings.colors.timesheetBreaks"))
        )
        .add(
          UICustomized(UICustomized.TYPE.COLOR_CHOOSER).add("id", CalendarSettings::vacationsColor.name)
            .add("label", translate("calendar.settings.colors.vacations"))
        )
        .add(UIAlert("calendar.settings.colors.vacations.info", color = UIColor.LIGHT, markdown = true))
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
    layout.watchFields.addAll(
      arrayOf(
        CalendarSettings::timesheetsColor.name,
        CalendarSettings::timesheetStatsColor.name,
        CalendarSettings::timesheetBreaksColor.name,
        CalendarSettings::vacationsColor.name,
      )
    )
    return FormLayoutData(settings, layout, createServerData(request))
  }

  @PostMapping(RestPaths.WATCH_FIELDS)
  fun watchFields(@Valid @RequestBody postData: PostData<CalendarSettings>): ResponseAction {
    if (postData.watchFieldsTriggered?.contains("employee") == false) {
      return ResponseAction(targetType = TargetType.NOTHING)
    }
    return ResponseAction(targetType = TargetType.NOTHING)
  }

}
