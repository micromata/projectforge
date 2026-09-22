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

package org.projectforge.rest.dto.datatable

/**
 * Request DTO for saving table grid state.
 * Sent from the frontend when grid state changes (column order, sizing, visibility, sorting, filters).
 */
class DataTableStateRequest {
    /**
     * Column order as list of field IDs.
     */
    var columnOrder: List<String>? = null

    /**
     * Column widths by field ID.
     */
    var columnSizing: Map<String, Int>? = null

    /**
     * Column visibility: maps field ID to false for hidden columns (absent = visible).
     */
    var columnVisibility: Map<String, Boolean>? = null

    /**
     * Pinned columns.
     */
    var columnPinning: DataTableColumnPinning? = null

    /**
     * Current sorting state.
     */
    var sorting: List<DataTableSortingEntry>? = null

    /**
     * Active column filters.
     */
    var columnFilters: List<DataTableColumnFilter>? = null

    /**
     * Selected number of rows per page.
     */
    var paginationPageSize: Int? = null
}

class DataTableColumnPinning {
    var left: List<String>? = null
    var right: List<String>? = null
}

class DataTableSortingEntry {
    var id: String? = null
    var desc: Boolean? = null
}

class DataTableColumnFilter {
    var id: String? = null
    var value: Any? = null
}
