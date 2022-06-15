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

package org.projectforge.ui

import kotlin.reflect.KProperty

/**
 * Twelve column grid system.
 */
open class UICol(
  /**
   * Length in grid system
   */
  var length: UILength? = null,
  /**
   * Offset in grid system
   */
  val offset: UILength? = null,
  val content: MutableList<UIElement> = mutableListOf(),
  type: UIElementType = UIElementType.COL,
  /**
   * Useless, if length is already given.
   */
  xs: Int? = null,
  sm: Int? = null,
  md: Int? = null,
  lg: Int? = null,
  var collapseTitle: String? = null,
) : UIElement(type), IUIContainer {

  constructor(xsLength: Int) : this(length = UILength(xsLength))

  init {
    if (length == null) {
      length = UILength(xs = xs, sm = sm, md = md, lg = lg)
    }
  }

  override fun add(element: UIElement): UICol {
    content.add(element)
    return this
  }

  fun add(index: Int, element: UIElement): UICol {
    content.add(index, element)
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

  fun add(layoutSettings: LayoutContext, vararg properties: KProperty<*>, createRowCol: Boolean = false): UICol {
    properties.forEach {
      val element = LayoutUtils.buildLabelInputElement(layoutSettings, it.name)
      if (element != null) {
        add(LayoutUtils.prepareElementToAdd(element, createRowCol))
      }
    }
    return this
  }
}
