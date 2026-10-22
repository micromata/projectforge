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

import org.projectforge.business.user.service.UserPrefService
import org.projectforge.rest.dto.datatable.DataTableStateRequest
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * For saving current gridState in user's pref.
 */
@Service
class AGGridSupport {
    @Autowired
    private lateinit var userPrefService: UserPrefService

    fun storeGridState(category: String, request: DataTableStateRequest) {
        val gridState = userPrefService.ensureEntry(category, USER_PREF_PARAM_GRID_STATE, GridState())

        request.columnOrder?.let { gridState.columnOrder = it }
        request.columnSizing?.let { gridState.columnSizing = it }
        request.columnVisibility?.let { gridState.columnVisibility = it }
        request.columnPinning?.let { gridState.columnPinning = it }
        request.sorting?.let { gridState.sorting = it }
        request.columnFilters?.let { gridState.columnFilters = it }
        request.paginationPageSize?.let { gridState.paginationPageSize = it }

        userPrefService.putEntry(category, USER_PREF_PARAM_GRID_STATE, gridState, true)
    }

    /**
     * The stored grid state, or null if the user has none yet.
     *
     * The task tree folds it into its column definitions instead ([restoreColumnsFromUserPref]).
     */
    fun getGridState(category: String): GridState? {
        return userPrefService.getEntry(category, USER_PREF_PARAM_GRID_STATE, GridState::class.java)
    }

    /**
     * Resets the grid state by storing an empty state in the userPrefService.
     */
    fun resetGridState(category: String) {
        val emptyGridState = GridState()
        userPrefService.putEntry(category, USER_PREF_PARAM_GRID_STATE, emptyGridState)
    }

    fun restoreColumnsFromUserPref(category: String, agGrid: UIAgGrid) {
        val gridState = getGridState(category) ?: return

        // Restore column order
        val columnOrder = gridState.columnOrder
        if (columnOrder != null && columnOrder.isNotEmpty()) {
            val lockedColumns = agGrid.columnDefs.filter { it.lockPosition != null }
            val unlockedColumnDefs = agGrid.columnDefs.filter { it.lockPosition == null }

            val reorderedUnlockedColumns = mutableListOf<UIAgGridColumnDef>()
            val processedColumns = mutableSetOf<String>()
            columnOrder.forEach { colId ->
                unlockedColumnDefs.find { it.field == colId }?.let { colDef ->
                    reorderedUnlockedColumns.add(colDef)
                    colDef.field?.let { processedColumns.add(it) }
                }
            }
            unlockedColumnDefs.forEach { colDef ->
                if (!processedColumns.contains(colDef.field)) {
                    reorderedUnlockedColumns.add(colDef)
                }
            }
            agGrid.columnDefs = (lockedColumns + reorderedUnlockedColumns).toMutableList()
        }

        // Restore column sizing
        val columnSizing = gridState.columnSizing
        if (columnSizing != null) {
            agGrid.columnDefs.forEach { colDef ->
                val field = colDef.field ?: return@forEach
                columnSizing[field]?.let { width ->
                    if (colDef.resizable != false) {
                        colDef.width = width
                    }
                }
            }
        }

        // Restore column visibility
        val columnVisibility = gridState.columnVisibility
        if (columnVisibility != null) {
            agGrid.columnDefs.forEach { colDef ->
                val field = colDef.field ?: return@forEach
                columnVisibility[field]?.let { visible ->
                    colDef.hide = !visible
                }
            }
        }

        // Restore column pinning
        val columnPinning = gridState.columnPinning
        if (columnPinning != null) {
            agGrid.columnDefs.forEach { colDef ->
                if (colDef.lockPosition != null) return@forEach
                val field = colDef.field ?: return@forEach
                colDef.pinned = when {
                    columnPinning.left?.contains(field) == true -> "left"
                    columnPinning.right?.contains(field) == true -> "right"
                    else -> null
                }
            }
        }

        // Restore sorting as sortModel (for frontend consumption)
        val sorting = gridState.sorting
        if (sorting != null && sorting.isNotEmpty()) {
            agGrid.sortModel = sorting.mapIndexedNotNull { index, entry ->
                val id = entry.id ?: return@mapIndexedNotNull null
                SortModelEntry(
                    colId = id,
                    sort = if (entry.desc == true) "desc" else "asc",
                    sortIndex = index,
                )
            }
        }
    }

    /**
     * Creates a ResponseAction for resetting grid state.
     * Includes columnDefs, sortModel, and an empty filterModel.
     */
    fun createResetGridStateResponse(agGrid: UIAgGrid?): ResponseAction {
        return ResponseAction(targetType = TargetType.UPDATE).apply {
            if (agGrid != null) {
                addVariable("columnDefs", agGrid.columnDefs)
                agGrid.sortModel?.let { addVariable("sortModel", it) }
                addVariable("filterModel", emptyMap<String, Any>())
            }
        }
    }

    companion object {
        const val USER_PREF_PARAM_GRID_STATE = "gridState"
    }
}
