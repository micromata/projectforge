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

package org.projectforge.common.html

class HtmlTable : HtmlElement("table") {
    class TD(content: String? = null) : HtmlElement("td", content = content)
    class TH(content: String? = null) : HtmlElement("th", content = content)
    class TR : HtmlElement("tr") {
        fun addTH(content: String? = null, cssClass: CssClass? = null): TH {
            return TH(content).also { th ->
                add(th)
                th.add(cssClass)
            }
        }

        fun addTD(content: String? = null, cssClass: CssClass? = null): TD {
            return TD(content).also { td ->
                add(td)
                td.add(cssClass)
            }
        }
    }

    private val thead = HtmlElement("thead")
    private val tbody = HtmlElement("tbody")

    init {
        add(thead)
        add(tbody)
    }

    fun addHeadRow(): TR {
        return TR().also { thead.add(it) }
    }

    fun addRow(cssClass: CssClass? = null): TR {
        return TR().also { tr ->
            tbody.add(tr)
            tr.add(cssClass)
        }
    }
}

