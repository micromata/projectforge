/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.ui

import com.fasterxml.jackson.annotation.JsonIgnore

data class UIList(
        /**
         * Needed to register elementVar during layout processing.
         */
        @JsonIgnore
        val lc: LayoutContext,
        /**
         * The path of the list in the data object.
         */
        val listId: String,
        /**
         * The name of an item of the list, usable by the child elements as prefix of id.
         */
        val elementVar: String,
        val content: MutableList<UIElement> = mutableListOf())
    : UIElement(UIElementType.LIST) {
    init {
        lc.registerListElement(elementVar, listId)
    }

    fun add(listEntry: UIElement): UIList {
        content.add(listEntry)
        return this
    }
}
