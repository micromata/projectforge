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

/**
 * Column def AgGrid
 */
open class UIAgGridColumnDef(
  val field: String,
  var id: String? = null,
  var headerName: String? = null,
  var sortable: Boolean = false,
  var filter: Boolean = false,
  var valueGetter: String? = null,
  var valueFormatter: String? = null,
  var type: String? = null,
) {
  enum class TYPE(val agType: String) { NUMERIC_COLUMN("numericColumn"), RIGHT_ALIGNED("rightAligned") }

  fun withType(type: TYPE): UIAgGridColumnDef {
    this.type = type.agType
    return this
  }
}
