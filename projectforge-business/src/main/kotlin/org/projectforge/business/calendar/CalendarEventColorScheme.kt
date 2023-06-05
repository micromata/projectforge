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

package org.projectforge.business.calendar

import org.projectforge.common.i18n.I18nEnum

/**
 * Used by CalendarStyle to calculate foreground and background colors.
 */
enum class CalendarEventColorScheme(
  val key: String) : I18nEnum {
  STANDARD("standard"),
  /**
   * No opacity, better for users with the need of higher contrasts.
   */
  CLASSIC("classic");

  override val i18nKey: String
    get() = "calendar.settings.colors.scheme.$key"
}
