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

import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringEscapeUtils

class TooltipBuilder() {
  private val sb = StringBuilder()
  private var endOfTable: Boolean = false

  init {
    sb.append("<table>")
  }

  fun addPropRow(label: String, value: String?, abbreviate: Boolean? = null, escapeHtml: Boolean = true, pre: Boolean = false): TooltipBuilder {
    if (value.isNullOrBlank()) {
      return this
    }
    var displayValue = if (abbreviate == true) {
      StringUtils.abbreviate(value, 60)
    } else {
      value
    }
    if (escapeHtml) {
      displayValue = StringEscapeUtils.escapeHtml4(displayValue)
    }
    if (pre && value.contains("\n")) {
      displayValue = "<pre>$displayValue</pre>"
    }
    sb.append("<tr><th>$label:</th><td>$displayValue</td></tr>")
    return this
  }

  override fun toString(): String {
    if (!endOfTable) {
      sb.append("</table>")
      endOfTable = false
    }
    return sb.toString()
  }
}
