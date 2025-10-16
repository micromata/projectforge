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

package org.projectforge.rest.core.aggrid

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.aggrid.AGColumnState
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MultiSelectionSupport
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

    fun storeColumnState(category: String, columnState: List<AGColumnState>) {
        val gridState = userPrefService.ensureEntry(category, USER_PREF_PARAM_GRID_STATE, GridState())
        gridState.columnState =
            columnState.map { ColumnStateEntry(colId = it.colId, hide = it.hide, width = it.width, pinned = it.pinned) }
        val newSortModel = mutableListOf<SortModelEntry>()
        columnState.forEach { entry ->
            val colId = entry.colId
            val sort = entry.sort
            val sortIndex = entry.sortIndex
            if (colId != null && sort != null && sortIndex != null) {
                newSortModel.add(SortModelEntry(colId, entry.sort, entry.sortIndex))
            }
        }
        gridState.sortModel = newSortModel
    }

    private fun getColumnState(category: String): List<ColumnStateEntry>? {
        return userPrefService.getEntry(category, USER_PREF_PARAM_GRID_STATE, GridState::class.java)?.columnState
    }

    private fun getSortModel(category: String): List<SortModelEntry>? {
        return userPrefService.getEntry(category, USER_PREF_PARAM_GRID_STATE, GridState::class.java)?.sortModel
    }

    /**
     * Deletes the grid state of the userPrefService.
     */
    fun resetGridState(category: String) {
        userPrefService.removeEntry(category, USER_PREF_PARAM_GRID_STATE)
    }

    fun prepareUIGrid4ListPage(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        pagesRest: AbstractPagesRest<*, *, *>,
        pageAfterMultiSelect: Class<out AbstractDynamicPageRest>? = null,
        userAccess: UILayout.UserAccess,
        rowClickUrl: String? = null,
    ): UIAgGrid {
        val agGrid = UIAgGrid.createUIResultSetTable()
        magicFilter.maxRows = QueryFilter.QUERY_FILTER_MAX_ROWS // Fix it from previous.
        agGrid.enablePagination()
        magicFilter.paginationPageSize?.let { agGrid.paginationPageSize = it }
        layout.add(agGrid)
        if (MultiSelectionSupport.isMultiSelection(request, magicFilter)) {
            prepareUIGrid4MultiSelectionListPage(request, layout, agGrid, pagesRest, pageAfterMultiSelect)
        } else if (userAccess.update == true) {
            val redirectUrl =
                rowClickUrl ?: "${PagesResolver.getEditPageUrl(pagesRest::class.java, absolute = true)}/id"
            agGrid.withRowClickRedirectUrl(redirectUrl)
            layout.add(UIAlert(message = "agGrid.sortInfo", color = UIColor.INFO, markdown = true))
            if (pageAfterMultiSelect != null) {
                layout.multiSelectionSupported = true
            }
            // Done for multiselect by prepareUIGrid4MultiSelectionListPage:
            agGrid.onColumnStatesChangedUrl =
                RestResolver.getRestUrl(pagesRest::class.java, RestPaths.SET_COLUMN_STATES)
        }
        return agGrid
    }

    fun prepareUIGrid4MultiSelectionListPage(
        request: HttpServletRequest,
        layout: UILayout,
        agGrid: UIAgGrid,
        callerRest: Any,
        pageAfterMultiSelect: Class<out AbstractDynamicPageRest>? = null,
    ) {
        MultiSelectionSupport.getSessionContext(
            request,
            callerRest::class.java
        )?.paginationPageSize?.let { paginationPageSize ->
            // pageSize was initially set by Wicket's list page. So use the same pagination size.
            agGrid.paginationPageSize = paginationPageSize
        }
        if (pageAfterMultiSelect != null) {
            agGrid.urlAfterMultiSelect =
                RestResolver.getRestUrl(pageAfterMultiSelect, AbstractMultiSelectedPage.URL_PATH_SELECTED)
        }
        agGrid.handleCancelUrl = RestResolver.getRestUrl(callerRest::class.java, RestPaths.CANCEL_MULTI_SELECTION)
        agGrid.selectionColumnDef = UIAgGridColumnDef().also {
            it.pinned = "left"
            it.resizable = false
            it.sortable = false
            it.filter = false
            it.width = 10
        }
        layout
            .add(
                UIAlert(
                    message = "multiselection.aggrid.selection.info.message",
                    title = "multiselection.aggrid.selection.info.title",
                    color = UIColor.INFO,
                    markdown = true,
                )
            )
        agGrid.onColumnStatesChangedUrl = RestResolver.getRestUrl(callerRest::class.java, RestPaths.SET_COLUMN_STATES)
    }

    fun restoreColumnsFromUserPref(category: String, agGrid: UIAgGrid) {
        val columnStates = getColumnState(category)
        if (columnStates != null) {
            // Separate locked and unlocked columns
            val lockedColumns = agGrid.columnDefs.filter { it.lockPosition != null }
            val unlockedColumnDefs = agGrid.columnDefs.filter { it.lockPosition == null }

            // Reorder only unlocked columns based on user preferences
            val reorderedUnlockedColumns = mutableListOf<UIAgGridColumnDef>()
            val processedColumns = mutableSetOf<String>()
            columnStates.forEach { columnState ->
                unlockedColumnDefs.find { it.field == columnState.colId }?.let { colDef ->
                    reorderedUnlockedColumns.add(colDef)
                    colDef.field?.let {
                        processedColumns.add(it)
                    }
                }
            }
            // Add unlocked columns not part of columnStates
            unlockedColumnDefs.forEach { colDef ->
                if (!processedColumns.contains(colDef.field)) {
                    reorderedUnlockedColumns.add(colDef)
                }
            }

            // Combine: locked columns first (in original order), then reordered unlocked columns
            agGrid.columnDefs = (lockedColumns + reorderedUnlockedColumns).toMutableList()

            // Restore width, hide, pinned for all columns
            agGrid.columnDefs.forEach { colDef ->
                columnStates.find { it.colId == colDef.field }?.let { columnState ->
                    // Only restore width if column is resizable
                    if (colDef.resizable != false) {
                        colDef.width = columnState.width
                    }
                    colDef.hide = columnState.hide
                    colDef.pinned = columnState.pinned
                }
            }
        }
        agGrid.sortModel = getSortModel(category)
    }

    companion object {
        const val USER_PREF_PARAM_GRID_STATE = "gridState"
    }
}
