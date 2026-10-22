/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.calendar.CalendarStyle
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import kotlin.reflect.KMutableProperty1

/**
 * The calendar's colour settings (independent of the filter), read and written as plain JSON by the settings
 * dialog of the projectforge-next calendar.
 */
@RestController
@RequestMapping("${Rest.URL}/calendarSettings")
class CalendarSettingsRest {
  @Autowired
  private lateinit var calendarSettingsService: CalendarSettingsService

  private fun validate(data: CalendarSettings): ResponseEntity<ResponseAction>? {
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

  /**
   * Plain-JSON read of the current settings for the Next.js calendar's settings dialog (the gear),
   * which merges the colours into its own modal. Returns the cloned settings with every colour filled by its
   * default (see [CalendarSettingsService.getSettings]).
   */
  @AccessChecked("Own user only (logged-in user's data/prefs)")
  @GetMapping("settings")
  fun getSettingsJson(): CalendarSettings {
    return calendarSettingsService.getSettings()
  }

  /**
   * Plain-JSON persist counterpart of [getSettingsJson]. Runs the colour validation
   * (406 with the field errors on a bad hex code) and, like the sibling `change*` endpoints, relies on
   * the `X-PF-CSRF-Token` header rather than a wrapped `PostData`. Answers with the canonical settings.
   */
  @AccessChecked("Own user only (logged-in user's data/prefs)")
  @PostMapping("settings")
  fun saveSettingsJson(@RequestBody settings: CalendarSettings): ResponseEntity<*> {
    validate(settings)?.let { return it }
    calendarSettingsService.persistSettings(settings)
    return ResponseEntity.ok(calendarSettingsService.getSettings())
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
