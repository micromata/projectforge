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

open class UICol(
        /**
         * Length in grid system (1-12)
         */
        val length: Int? = null,
        /**
         * Length for small screens (1-12)
         */
        val smLength: Int? = null,
        /**
         * Length for small screens (1-12)
         */
        val mdLength: Int? = null,
        /**
         * Length for large screens (1-12)
         */
        val lgLength: Int? = null,
        /**
         * Length for large screens (1-12)
         */
        val xlLength: Int? = null,
        val content: MutableList<UIElement> = mutableListOf(),
        type: UIElementType = UIElementType.COL)
    : UIElement(type) {
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
