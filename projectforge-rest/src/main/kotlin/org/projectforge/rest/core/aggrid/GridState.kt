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

package org.projectforge.rest.core.aggrid

import org.projectforge.rest.dto.datatable.DataTableColumnFilter
import org.projectforge.rest.dto.datatable.DataTableColumnPinning
import org.projectforge.rest.dto.datatable.DataTableSortingEntry

/**
 * For saving current gridState in user's pref.
 * Stores TanStack Table native state format.
 */
class GridState {
    var columnOrder: List<String>? = null
    var columnSizing: Map<String, Int>? = null
    var columnVisibility: Map<String, Boolean>? = null
    var columnPinning: DataTableColumnPinning? = null
    var sorting: List<DataTableSortingEntry>? = null
    var columnFilters: List<DataTableColumnFilter>? = null

    /**
     * Number of rows per page the user selected. Null for pages the user never changed it on, meaning
     * the page's own default applies.
     */
    var paginationPageSize: Int? = null
}
