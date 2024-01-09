/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.dto.aggrid

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * https://www.ag-grid.com/javascript-data-grid/column-state/
 */
class AGColumnState {
  var colId: String? = null
  var hide: Boolean? = null
  var pinned: String? = null
  var width: Int? = null
  var flex: Int? = null
  var sort: String? = null
  var sortIndex: Int? = null

  @JsonIgnore
  var aggFunc: Any? = null
  var pivot: Boolean? = null
  var pivotIndex: Int? = null
  var rowGroup: Boolean? = null
  var rowGroupIndex: Int? = null
}
