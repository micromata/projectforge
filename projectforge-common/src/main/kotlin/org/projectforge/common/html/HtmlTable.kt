/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
        fun addTH(content: String? = null, cls: CssClass? = null): TH {
            return addTH(content, cls?.let { arrayOf(it) })
        }

        fun addTH(content: String? = null, classes: Array<CssClass>?): TH {
            return TH(content).also { th ->
                add(th)
                if (!classes.isNullOrEmpty()) {
                    th.attr("class", classes.joinToString(separator = " ") { it.cls })
                }
            }
        }

        fun addTD(content: String? = null, cls: CssClass? = null): TD {
            return addTD(content, cls?.let { arrayOf(it) })
        }

        fun addTD(content: String? = null, classes: Array<CssClass>?): TD {
            return TD(content).also { td ->
                add(td)
                if (!classes.isNullOrEmpty()) {
                    td.attr("class", classes.joinToString(separator = " ") { it.cls })
                }
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

    fun addRow(): TR {
        return TR().also { tbody.add(it) }
    }
}

