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

fun main() {
    val doc = HtmlDocument("Title")
    val table = HtmlTable()
    val row = table.addRow()
    row.addTH("Header 1")
    row.addTH("Header 2")
    val row2 = table.addRow()
    row2.addTD("Cell 1")
    row2.addTD("Cell 2")
    doc.add(table)
    doc.add(Html.Alert(Html.Alert.Type.INFO, "This is an info message."))
    println(doc.toString())
}

