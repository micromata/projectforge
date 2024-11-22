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

package org.projectforge.rest.calendar

import org.projectforge.Constants
import org.projectforge.business.calendar.CalendarEventColorScheme
import org.projectforge.business.calendar.CalendarStyle
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import kotlin.reflect.KMutableProperty1

/**
 * Page and services for settings for the calendar (independent of filter).
 */
@RestController
@RequestMapping("${Rest.URL}/calendarSettings")
class CalendarSettingsPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var calendarSettingsRest: CalendarSettingsService

  fun validate(data: CalendarSettings): ResponseEntity<ResponseAction>? {
    val validationErrors = mutableListOf<ValidationError>()
    checkColorCode(validationErrors, CalendarSettings::timesheetsColor, data.timesheetsColor)
    checkColorCode(validationErrors, CalendarSettings::timesheetsBreaksColor, data.timesheetsBreaksColor)
    checkColorCode(validationErrors, CalendarSettings::timesheetsStatsColor, data.timesheetsStatsColor)
    checkColorCode(validationErrors, CalendarSettings::vacationsColor, data.vacationsColor)
    if (validationErrors.isEmpty()) {
      return null
    }
    return ResponseEntity(ResponseAction(validationErrors = validationErrors), HttpStatus.NOT_ACCEPTABLE)
  }

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
            .add("defaultColor", CalendarSettings.TIMESHEETS_DEFAULT_COLOR)
        )
        .add(
          UICustomized(UICustomized.TYPE.COLOR_CHOOSER)
            .add("id", CalendarSettings::timesheetsStatsColor.name)
            .add("label", translate("calendar.settings.colors.timesheetStats"))
            .add("defaultColor", CalendarSettings.TIMESHEETS_STATS_DEFAULT_COLOR)
        )
        .add(
          UICustomized(UICustomized.TYPE.COLOR_CHOOSER)
            .add("id", CalendarSettings::timesheetsBreaksColor.name)
            .add("label", translate("calendar.settings.colors.timesheetBreaks"))
            .add("defaultColor", CalendarSettings.TIMESHEETS_BREAKS_DEFAULT_COLOR)
        )
        .add(
          UICustomized(UICustomized.TYPE.COLOR_CHOOSER)
            .add("id", CalendarSettings::vacationsColor.name)
            .add("label", translate("calendar.settings.colors.vacations"))
            .add("defaultColor", CalendarSettings.VACATIONS_DEFAULT_COLOR)
        )
        .add(UIAlert("calendar.settings.colors.vacations.info", color = UIColor.LIGHT, markdown = true))
        .add(
          UISelect<CalendarEventColorScheme>(
            CalendarSettings::colorScheme.name,
            label = "calendar.settings.colors.scheme",
            required = false,
          ).buildValues(
            CalendarEventColorScheme::class.java
          )
        )
        .add(
          UICheckbox(
            CalendarSettings::alternateHoursBackground.name,
            label = "calendar.settings.alternateHoursBackground",
            tooltip = "calendar.settings.alternateHoursBackground.toolip",
          )
        )
    )
    layout.add(
      UIButton.createCancelButton(responseAction = ResponseAction("/${Constants.REACT_APP_PATH}calendar"))
    ).add(
      UIButton.createDefaultButton(
        "save",
        title = "save",
        responseAction = ResponseAction(this.getRestPath(RestPaths.SAVE), targetType = TargetType.POST)
      )
    )
    LayoutUtils.process(layout)
    layout.watchFields.addAll(
      arrayOf(
        CalendarSettings::timesheetsColor.name,
        CalendarSettings::timesheetsStatsColor.name,
        CalendarSettings::timesheetsBreaksColor.name,
        CalendarSettings::vacationsColor.name,
      )
    )
    layout.addTranslations("default", "finish", "select")
    return FormLayoutData(settings, layout, createServerData(request))
  }

  @PostMapping(RestPaths.WATCH_FIELDS)
  fun watchFields(@Valid @RequestBody postData: PostData<CalendarSettings>): ResponseEntity<*> {
    val data = postData.data
    validate(data)?.let { return it }
    return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
  }

  @PostMapping(RestPaths.SAVE)
  fun save(
    request: HttpServletRequest,
    @RequestBody postData: PostData<CalendarSettings>
  ): ResponseEntity<ResponseAction> {
    validateCsrfToken(request, postData)?.let { return it }
    val data = postData.data
    validate(data)?.let { return it }
    calendarSettingsRest.persistSettings(data)
    return ResponseEntity.ok(
      ResponseAction("/${Constants.REACT_APP_PATH}calendar?hash=${NumberHelper.getSecureRandomAlphanumeric(4)}")
    )
  }

  private fun checkColorCode(
    validationErrors: MutableList<ValidationError>,
    fieldId: KMutableProperty1<*, *>,
    color: String?
  ) {
    if (color.isNullOrBlank()) {
      // Nothing to validate
      return
    }
    val checkColor = color.trim().lowercase()
    if (!CalendarStyle.validateHexCode(checkColor)) {
      validationErrors.add(ValidationError(translate("color.error.unknownFormat"), fieldId = fieldId.name))
    }
  }
}
