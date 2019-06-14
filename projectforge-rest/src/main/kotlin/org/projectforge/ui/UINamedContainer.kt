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

/**
 * A named container represents a bunch of UIElements usable by the UI referenced by the given name (id). It may contain
 * e. g. filter settings in a list view.
 */
data class UINamedContainer(
        /** The name of the container. */
        val id: String,
        val content: MutableList<UIElement> = mutableListOf())
    : UIElement(UIElementType.NAMED_CONTAINER) {

    fun add(element: UIElement): UINamedContainer {
        content.add(element)
        return this
    }
}
