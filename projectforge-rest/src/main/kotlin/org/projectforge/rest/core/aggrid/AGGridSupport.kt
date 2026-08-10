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

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.datatable.DataTableStateRequest
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
     * Pages built from a [UILayout] don't need this: [restoreColumnsFromUserPref] folds the state
     * back into the column definitions for them. Hand-built frontend pages have no layout to fold
     * it into and read the state directly instead (see AbstractPagesRest.getColumnStates).
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

    /**
     * Prepares an AG-Grid for a list page, handling multi-selection if applicable.
     */
    fun prepareUIGrid4ListPage(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        pagesRest: AbstractPagesRest<*, *, *>,
        pageAfterMultiSelect: Class<out AbstractDynamicPageRest>? = null,
        userAccess: UILayout.UserAccess,
        rowClickUrl: String? = null,
        legendText: String? = null,
    ): UIAgGrid {
        val agGrid = UIAgGrid.createUIResultSetTable()
        magicFilter.maxRows = QueryFilter.QUERY_FILTER_MAX_ROWS
        agGrid.enablePagination()
        magicFilter.paginationPageSize?.let { agGrid.paginationPageSize = it }
        // The page size the user selected last, stored along with the column state. Applied here and not
        // in [restoreColumnsFromUserPref], because that runs after this method (LayoutUtils.processListPage)
        // and would override the page size a multi selection deliberately remembered in the session below.
        getGridState(pagesRest.category)?.paginationPageSize?.let { agGrid.paginationPageSize = it }
        layout.add(agGrid)
        if (MultiSelectionSupport.isMultiSelection(request, magicFilter)) {
            prepareUIGrid4MultiSelectionListPage(request, layout, agGrid, pagesRest, pageAfterMultiSelect)
        } else {
            if (userAccess.update == true) {
                // The edit page with the id placeholder, resolved per row by both frontends. Not the new-entry
                // url with "/id" appended: that only happened to match for the generic React shape
                // (react/<category>/edit + /id), while e.g. book uses next/book/new and next/book/:id, which
                // turned a row click into next/book/new/<id> - the empty *new* form. Asking the pages rest also
                // honours its getStandardEditPage() override (address, script, project, poll ...) and keeps a
                // user who is looking at the legacy list of a migrated page in the legacy app.
                val redirectUrl = rowClickUrl ?: "/${pagesRest.getEditPage(request)}"
                agGrid.withRowClickRedirectUrl(redirectUrl, openModal = pagesRest.useModalEditDialog)
                if (pageAfterMultiSelect != null) {
                    layout.multiSelectionSupported = true
                }
            }
            val message = if (legendText.isNullOrBlank()) {
                "agGrid.sortInfo"
            } else {
                "'$legendText\n${translate("agGrid.sortInfo")}"
            }
            layout.add(UIAlert(message = message, color = UIColor.INFO, markdown = true))
            agGrid.onColumnStatesChangedUrl =
                RestResolver.getRestUrl(pagesRest::class.java, RestPaths.SET_COLUMN_STATES)
            agGrid.resetGridStateUrl =
                RestResolver.getRestUrl(pagesRest::class.java, "resetGridState")
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
        agGrid.resetGridStateUrl = RestResolver.getRestUrl(callerRest::class.java, "resetGridState")
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
     * Finds the AG Grid element in a UI layout.
     * Searches recursively through all UI elements and containers.
     */
    fun findAgGridElement(layout: UILayout?): UIAgGrid? {
        layout ?: return null
        findAgGridInContent(layout.layout)?.let { return it }
        return findAgGridInContent(layout.namedContainers.flatMap { it.content })
    }

    /**
     * Recursively searches for UIAgGrid in a list of UI elements.
     */
    fun findAgGridInContent(content: List<UIElement>): UIAgGrid? {
        for (element in content) {
            when (element) {
                is UIAgGrid -> return element
                is UIGroup -> findAgGridInContent(element.content)?.let { return it }
                is UIInlineGroup -> findAgGridInContent(element.content)?.let { return it }
                is UIRow -> findAgGridInContent(element.content)?.let { return it }
                is UICol -> findAgGridInContent(element.content)?.let { return it }
                is UIFieldset -> findAgGridInContent(element.content)?.let { return it }
                is UIList -> findAgGridInContent(element.content)?.let { return it }
            }
        }
        return null
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
