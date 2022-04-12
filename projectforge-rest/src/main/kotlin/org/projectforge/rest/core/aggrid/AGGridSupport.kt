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

package org.projectforge.rest.core.aggrid

import org.projectforge.business.user.service.UserPrefService
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QUERY_FILTER_MAX_ROWS
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
import javax.servlet.http.HttpServletRequest

/**
 * For saving current gridState in user's pref.
 */
@Service
class AGGridSupport {
  @Autowired
  private lateinit var userPrefService: UserPrefService

  fun storeColumnStates(category: String, columStates: List<AGColumnState>) {
    val gridState = userPrefService.ensureEntry(category, USER_PREF_PARAM_GRID_STATE, GridState())
    gridState.columnStates = columStates.map { ColumnState(it.colId, it.hide, it.width, it.sort, it.sortIndex) }
  }

  fun storeGridState(category: String, gridState: GridState) {
    userPrefService.putEntry(category, USER_PREF_PARAM_GRID_STATE, gridState, true)
  }

  fun getColumnStates(category: String): List<ColumnState>? {
    return userPrefService.getEntry(category, USER_PREF_PARAM_GRID_STATE, GridState::class.java)?.columnStates
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
  ): UIAgGrid {
    val table = UIAgGrid.createUIResultSetTable()
    magicFilter.maxRows = QUERY_FILTER_MAX_ROWS // Fix it from previous.
    table.enablePagination()
    magicFilter.paginationPageSize?.let { table.paginationPageSize = it }
    layout.add(table)
    if (MultiSelectionSupport.isMultiSelection(request, magicFilter)) {
      layout.hideSearchFilter = true
      if (pageAfterMultiSelect != null) {
        table.urlAfterMultiSelect =
          RestResolver.getRestUrl(pageAfterMultiSelect, AbstractMultiSelectedPage.URL_PATH_SELECTED)
      }
      table.handleCancelUrl = RestResolver.getRestUrl(pagesRest::class.java, RestPaths.CANCEL_MULTI_SELECTION)
      layout
        .add(
          UIAlert(
            message = "multiselection.aggrid.selection.info.message",
            title = "multiselection.aggrid.selection.info.title",
            color = UIColor.INFO,
            markdown = true,
          )
        )
    } else {
      table.withSingleRowClick()
      table.withRowClickRedirectUrl("${PagesResolver.getEditPageUrl(pagesRest::class.java, absolute = true)}/id")
      layout.add(UIAlert(message = "agGrid.sortInfo", color = UIColor.INFO, markdown = true))
    }
    table.onColumnStatesChangedUrl = RestResolver.getRestUrl(pagesRest::class.java, RestPaths.UPDATE_COLUMN_STATES)
    return table
  }

  fun restoreColumnsFromUserPref(category: String, agGrid: UIAgGrid) {
    val columnStates = getColumnStates(category) ?: return // Nothing to-do (initial state)
    val reorderedColumns = mutableListOf<UIAgGridColumnDef>()
    val processedColumns = mutableSetOf<String>()
    columnStates.forEach { columnState ->
      agGrid.columnDefs.find { it.field == columnState.colId && columnState.hide != true }?.let { colDef ->
        // ColumnDef found:
        reorderedColumns.add(colDef)
        processedColumns.add(colDef.field)
      }
    }
    // Add columns not part of columnStates
    agGrid.columnDefs.forEach { colDef ->
      if (!processedColumns.contains(colDef.field)) {
        reorderedColumns.add(colDef)
      }
    }
    agGrid.columnDefs = reorderedColumns
    agGrid.columnDefs // reorder
    agGrid.columnDefs.forEach { colDef ->
      columnStates.find { it.colId == colDef.field }?.let { columnState ->
        colDef.initialWidth = columnState.width
        colDef.initialSort = columnState.sort
        colDef.initialSortIndex = columnState.sortIndex
      }
    }
  }

  companion object {
    const val USER_PREF_PARAM_GRID_STATE = "gridState"
  }
}
