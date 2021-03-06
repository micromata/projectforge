/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

/**
 * Twelve column grid system.
 */
open class UICol(
        /**
         * Length in grid system
         */
        val length: UILength? = null,
        /**
         * Offset in grid system
         */
        val offset: UILength? = null,
        val content: MutableList<UIElement> = mutableListOf(),
        type: UIElementType = UIElementType.COL)
    : UIElement(type) {

    constructor(xsLength: Int): this(length = UILength(xsLength))

    fun add(element: UIElement): UICol {
        content.add(element)
        return this
    }

    /**
     * Convenient method for adding a bunch of UIInput fields with the given ids.
     * @param createRowCol If true (default), the elements will be surrounded with [UIRow] and [UICol] each, otherwise not.
     */
    fun add(layoutSettings: LayoutContext, vararg ids: String, createRowCol: Boolean = false): UICol {
        ids.forEach {
            val element = LayoutUtils.buildLabelInputElement(layoutSettings, it)
            if (element != null) {
                add(LayoutUtils.prepareElementToAdd(element, createRowCol))
            }
        }
        return this
    }
}
