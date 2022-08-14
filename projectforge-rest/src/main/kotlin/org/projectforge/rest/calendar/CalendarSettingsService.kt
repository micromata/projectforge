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

import org.projectforge.business.user.service.UserPrefService
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Page and services for settings for the calendar (independent of filter).
 */
@Service
class CalendarSettingsService : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var userPrefService: UserPrefService

  private val PREF_NAME = "settings"

  /**
   * Doesn't persist the calendar settings. Will get a clone of the persisted settings or a new settings object.
   * You have to persist modified settings through persistSettings.
   */
  internal fun getSettings(): CalendarSettings {
    return userPrefService.getEntry(CalendarFilterServicesRest.PREF_AREA, PREF_NAME, CalendarSettings::class.java)
      ?.cloneWithDefaultValues()
      ?: CalendarSettings()
  }

  /**
   * Default values will not be persisted (null values instead).
   */
  internal fun persistSettings(settings: CalendarSettings) {
    var storedSettings =
      userPrefService.getEntry(CalendarFilterServicesRest.PREF_AREA, PREF_NAME, CalendarSettings::class.java)
    if (storedSettings == null) {
      storedSettings = CalendarSettings()
      userPrefService.putEntry(CalendarFilterServicesRest.PREF_AREA, PREF_NAME, storedSettings)
    }
    storedSettings.copyWithoutDefaultsFrom(settings)
  }
}
