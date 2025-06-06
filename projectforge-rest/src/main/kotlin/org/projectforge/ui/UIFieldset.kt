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

package org.projectforge.ui

class UIFieldset(
  /**
   * Length in grid system
   */
  length: UILength? = null,
  /**
   * Offset in grid system
   */
  offset: UILength? = null,
  var title: String? = null,
  /**
   * Useless, if length is already given.
   */
  xs: Int? = null,
  sm: Int? = null,
  md: Int? = null,
  lg: Int? = null,
) :
  UICol(length ?: UILength(xs = xs, sm = sm, md = md, lg = lg), offset, type = UIElementType.FIELDSET) {

  constructor(xsLength: Int, title: String? = null) : this(length = UILength(xsLength), title = title)
}
