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

package org.projectforge.framework.utils

import org.projectforge.framework.i18n.translate

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MarkdownBuilder {
  private val sb = StringBuilder()

  enum class Color(val color: String) { RED("red"), BLUE("blue"), GREEN("green") }

  fun h3(text: String): MarkdownBuilder {
    sb.append("### ").appendLine(text).appendLine()
    return this
  }

  fun emptyLine(): MarkdownBuilder {
    first = true
    sb.appendLine()
    return this
  }

  fun append(text: String?): MarkdownBuilder {
    sb.append(text ?: "")
    return this
  }

  fun appendLine(text: String? = null): MarkdownBuilder {
    first = true
    sb.appendLine(text ?: "")
    return this
  }

  /**
   * @return this for chaining.
   */
  fun beginTable(vararg header: String?): MarkdownBuilder {
    first = true
    row(*header)
    header.forEach { sb.append("---").append(" | ") }
    sb.appendLine()
    return this
  }

  fun row(vararg cell: String?): MarkdownBuilder {
    first = true
    sb.append("| ")
    cell.forEach { sb.append(it ?: "").append(" | ") }
    sb.appendLine()
    return this
  }

  fun endTable(): MarkdownBuilder {
    first = true
    sb.appendLine()
    return this
  }

  private var first = true

  @JvmOverloads
  fun appendPipedValue(i18nKey: String, value: String, color: Color? = null, totalValue: String? = null) {
    ensureSeparator()
    if (color != null) {
      sb.append("<span style=\"color:${color.color};\">")
    }
    sb.append(translate(i18nKey)).append(": ").append(value)
    if (!totalValue.isNullOrBlank()) {
      sb.append("/").append(totalValue)
    }
    if (color != null) {
      sb.append("</span>")
    }
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
