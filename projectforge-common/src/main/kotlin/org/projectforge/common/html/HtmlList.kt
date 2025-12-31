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

class HtmlList(type: Type = Type.UNORDERED) : HtmlElement(type.cls) {
    enum class Type(val cls: String) {
        UNORDERED("ul"),
        ORDERED("ol")
    }

    class LI(var parent: HtmlList, content: String? = null) : HtmlElement("li", content = content) {
        fun addItem(
            text: String? = null,
            bold: Boolean = false,
            replaceNewlinesByBr: Boolean = true,
            vararg cssClasses: CssClass,
        ): LI {
            return parent.addItem(text, bold, replaceNewlinesByBr, *cssClasses)
        }
    }

    fun addItem(
        text: String? = null,
        bold: Boolean = false,
        replaceNewlinesByBr: Boolean = true,
        vararg cssClasses: CssClass,
    ): LI {
        return LI(this).also {
            super.add(it)
            text?.let { t ->
                it.add(t, bold, replaceNewlinesByBr, *cssClasses)
            }
        }
    }

    override fun add(child: HtmlElement): HtmlElement {
        throw IllegalArgumentException("HtmlList does not support adding own children.")
    }

    override fun add(
        text: String,
        bold: Boolean,
        replaceNewlinesByBr: Boolean,
        vararg cssClasses: CssClass
    ): LI {
        return addItem(text, bold, replaceNewlinesByBr, *cssClasses)
    }
}

