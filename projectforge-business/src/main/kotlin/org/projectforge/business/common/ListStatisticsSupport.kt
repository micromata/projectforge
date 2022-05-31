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

package org.projectforge.business.common

import org.projectforge.framework.i18n.translate

/**
 * For displaying statistics on result page (markdown).
 */
class ListStatisticsSupport {
  private val sb = java.lang.StringBuilder()

  private var first = true

  enum class Color(val color: String) { RED("red"), BLUE("blue") }

  val asMarkdown: String
    get() = sb.toString()

  fun append(i18nKey: String, value: String) {
    ensureSeparator()
    sb.append(translate(i18nKey)).append(": ").append(value)
  }

  fun append(i18nKey: String, value: String, color: Color) {
    sb.append("<span style=\"color:${color.color};\">")
    append(i18nKey, value)
    sb.append("</span>")
  }

  private fun ensureSeparator() {
    if (first) {
      first = false
    } else {
      sb.append(" | ")
    }
  }

  override fun toString(): String {
    return sb.toString()
  }
}
