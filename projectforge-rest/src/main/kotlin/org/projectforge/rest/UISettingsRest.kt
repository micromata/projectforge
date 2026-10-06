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
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Per-user UI preferences of the Next.js frontend that have no counterpart in [org.projectforge.rest.MyAccountPageRest]
 * because they only steer the client's appearance, not the account.
 *
 * The colour theme (light/dark/system) and the tile layouts of the chart dashboards. Persisted per user through [UserPrefService] (no DB migration),
 * so the choice follows the user across devices and browsers. Reads are plain JSON; the state-changing POST relies on
 * the central `X-PF-CSRF-Token` protection (see `RestCsrfProtection`), like the calendar's `saveSettingsJson`.
 */
@RestController
@RequestMapping("${Rest.URL}/uiSettings")
class UISettingsRest {
  @Autowired
  private lateinit var userPrefService: UserPrefService

  @AccessChecked("Own user only (logged-in user's data/prefs)")
  @GetMapping("theme")
  fun getTheme(): UIThemeSettings {
    return userPrefService.getEntry(PREF_AREA, PREF_NAME_THEME, UIThemeSettings::class.java)
      ?.let { UIThemeSettings(normalize(it.theme)) }
      ?: UIThemeSettings(DEFAULT_THEME)
  }

  @AccessChecked("Own user only (logged-in user's data/prefs)")
  @PostMapping("theme")
  fun setTheme(@RequestBody settings: UIThemeSettings): ResponseEntity<UIThemeSettings> {
    val value = normalize(settings.theme)
    userPrefService.putEntry(PREF_AREA, PREF_NAME_THEME, UIThemeSettings(value))
    return ResponseEntity.ok(UIThemeSettings(value))
  }

  /**
   * The stored tile layout of the chart dashboard [id] (one per page, e.g. `liquidity.forecast`); an empty layout
   * if the user never arranged it, which the client fills with the defaults of its tiles.
   */
  @AccessChecked("Own user only (logged-in user's data/prefs)")
  @GetMapping("dashboard/{id}")
  fun getDashboard(@PathVariable id: String): ResponseEntity<DashboardLayout> {
    if (!isValidId(id)) return ResponseEntity.badRequest().build()
    val stored = userPrefService.getEntry(PREF_AREA, dashboardPrefName(id), DashboardLayout::class.java)
    return ResponseEntity.ok(normalize(stored))
  }

  /** Stores the tile layout of dashboard [id]; an empty layout resets it to the client's defaults. */
  @AccessChecked("Own user only (logged-in user's data/prefs)")
  @PostMapping("dashboard/{id}")
  fun setDashboard(@PathVariable id: String, @RequestBody layout: DashboardLayout): ResponseEntity<DashboardLayout> {
    if (!isValidId(id)) return ResponseEntity.badRequest().build()
    val value = normalize(layout)
    userPrefService.putEntry(PREF_AREA, dashboardPrefName(id), value)
    return ResponseEntity.ok(value)
  }

  /**
   * Keeps only what the client may send: valid, distinct tile ids, known sizes (others become `null`, i.e. the
   * tile's default) and at most [MAX_TILES] tiles — the value is stored as is, so nothing else may get in.
   */
  internal fun normalize(layout: DashboardLayout?): DashboardLayout {
    val tiles = layout?.tiles.orEmpty()
      .filter { isValidId(it.id) }
      .distinctBy { it.id }
      .take(MAX_TILES)
      .map {
        DashboardTileLayout(
          id = it.id,
          width = it.width?.takeIf { w -> w in ALLOWED_WIDTHS },
          height = it.height?.takeIf { h -> h in ALLOWED_HEIGHTS },
          hidden = it.hidden == true,
        )
      }
    return DashboardLayout(tiles.toMutableList())
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

    const val MAX_TILES = 50
    val ALLOWED_WIDTHS = setOf("third", "half", "twoThirds", "full")
    val ALLOWED_HEIGHTS = setOf("S", "M", "L")
    private val ID_REGEX = Regex("[a-zA-Z0-9._-]{1,64}")

    internal fun isValidId(id: String?): Boolean = id != null && ID_REGEX.matches(id)

    internal fun dashboardPrefName(id: String) = "dashboard.$id"
  }
}

/** Serializable value stored in the user's preferences; a class (not a bare String) so it can grow without a migration. */
class UIThemeSettings(var theme: String? = null)

/**
 * The user's arrangement of a chart dashboard: its tiles in display order. Tiles the client defines but the
 * layout doesn't list are shown with their defaults, listed ids the client no longer knows are ignored.
 */
class DashboardLayout(var tiles: MutableList<DashboardTileLayout> = mutableListOf())

/** One tile of a [DashboardLayout]; `null` width/height mean the tile's default size. */
class DashboardTileLayout(
  var id: String? = null,
  /** One of [UISettingsRest.ALLOWED_WIDTHS]. */
  var width: String? = null,
  /** One of [UISettingsRest.ALLOWED_HEIGHTS]. */
  var height: String? = null,
  var hidden: Boolean? = null,
)
