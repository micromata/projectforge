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

package org.projectforge.rest

import org.projectforge.business.user.service.UserPrefService
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Per-user UI preferences of the Next.js frontend that have no counterpart in [org.projectforge.rest.MyAccountPageRest]
 * because they only steer the client's appearance, not the account.
 *
 * Currently only the colour theme (light/dark/system). Persisted per user through [UserPrefService] (no DB migration),
 * so the choice follows the user across devices and browsers. Reads are plain JSON; the state-changing POST relies on
 * the central `X-PF-CSRF-Token` protection (see `RestCsrfProtection`), like the calendar's `saveSettingsJson`.
 */
@RestController
@RequestMapping("${Rest.URL}/uiSettings")
class UISettingsRest {
  @Autowired
  private lateinit var userPrefService: UserPrefService

  @GetMapping("theme")
  fun getTheme(): UIThemeSettings {
    return userPrefService.getEntry(PREF_AREA, PREF_NAME_THEME, UIThemeSettings::class.java)
      ?.let { UIThemeSettings(normalize(it.theme)) }
      ?: UIThemeSettings(DEFAULT_THEME)
  }

  @PostMapping("theme")
  fun setTheme(@RequestBody settings: UIThemeSettings): ResponseEntity<UIThemeSettings> {
    val value = normalize(settings.theme)
    userPrefService.putEntry(PREF_AREA, PREF_NAME_THEME, UIThemeSettings(value))
    return ResponseEntity.ok(UIThemeSettings(value))
  }

  /** Falls back to [DEFAULT_THEME] for anything the client shouldn't be sending, so a bad value can't be stored. */
  private fun normalize(theme: String?): String {
    return theme?.takeIf { it in ALLOWED_THEMES } ?: DEFAULT_THEME
  }

  companion object {
    const val PREF_AREA = "nextUI"
    const val PREF_NAME_THEME = "theme"
    const val DEFAULT_THEME = "system"
    val ALLOWED_THEMES = setOf("light", "dark", "system")
  }
}

/** Serializable value stored in the user's preferences; a class (not a bare String) so it can grow without a migration. */
class UIThemeSettings(var theme: String? = null)
