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

package org.projectforge.ui

import mu.KotlinLogging
import kotlin.reflect.KProperty

private val log = KotlinLogging.logger {}

/**
 * Helper methods for creating layouts.
 */
object LayoutBuilder {

  fun createRowWithColumns(colSize: UILength, vararg element: UIElement): UIRow {
    val row = UIRow()
    element.forEach {
      row.add(
        UICol(colSize)
          .add(it)
      )
    }
    return row
  }

  /**
   * Convenient method for adding a bunch of UIInput fields with the given ids.
   */
  fun createElement(layoutSettings: LayoutContext, property: KProperty<*>): UIElement {
    return createElement(layoutSettings, property.name)
  }

  /**
   * Convenient method for adding a bunch of UIInput fields with the given ids.
   */
  fun createElement(layoutSettings: LayoutContext, name: String): UIElement {
    return LayoutUtils.buildLabelInputElement(layoutSettings, name)
  }
}
