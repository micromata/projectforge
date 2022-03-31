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

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.multiselect.MultiSelectionSupport
import java.io.Serializable
import javax.servlet.http.HttpServletRequest

/**
 * Table using AgGrid
 */
open class UIAgGrid(
  val id: String,
  val columnDefs: MutableList<UIAgGridColumnDef> = mutableListOf(),
  val listPageTable: Boolean = false,
  var rowSelection: String? = null, // multiple
  var rowMultiSelectWithClick: Boolean? = null,

  ) : UIElement(if (listPageTable) UIElementType.AG_GRID_LIST_PAGE else UIElementType.AG_GRID) {
  var multiSelectButtonTitle: String? = null

  /**
   * This url should be called with all selected rows to proceed the user action (mass update, export etc.)
   */
  var urlAfterMultiSelect: String? = null

  /**
   * Tell the client, which entities were selected (for recovering, e. g. after reload or back button).
   */
  var selectedEntities: Collection<Serializable>? = null

  companion object {
    @JvmStatic
    fun createUIResultSetTable(): UIAgGrid {
      return UIAgGrid("resultSet", listPageTable = true)
    }
  }

  init {
    if (!listPageTable) {
      throw IllegalArgumentException("UIAgGrid.listPageTable == false not yet supported by jsx.")
    }
  }

  fun add(column: UIAgGridColumnDef): UIAgGrid {
    columnDefs.add(column)
    return this
  }

  /**
   * For adding columns with the given ids
   * @param valueGetter Make only sense, if no multiple fields are given.
   * @param lcField Make only sense, if no multiple fields are given.
   * @return this for chaining.
   */
  fun add(
    lc: LayoutContext,
    vararg columnIds: String,
    sortable: Boolean = true,
    width: Int? = null,
    headerName: String? = null,
    valueFormatter: Formatter? = null,
    valueGetter: String? = null,
    lcField: String? = null,
  ): UIAgGrid {
    columnIds.forEach {
      add(
        UIAgGridColumnDef.createCol(
          lc,
          field = it,
          sortable = sortable,
          width = width,
          headerName = headerName,
          valueGetter = valueGetter,
          valueFormatter = valueFormatter,
          lcField = lcField ?: it,
        )
      )
    }
    return this
  }

  fun getColumnDefById(field: String): UIAgGridColumnDef {
    return columnDefs.find { it.field == field }!!
  }

  /**
   * @return this for chaining.
   */
  fun withMultiRowSelection(
    request: HttpServletRequest,
    magicFilter: MagicFilter
  ): UIAgGrid {
    val multiSelectionMode = MultiSelectionSupport.isMultiSelection(request, magicFilter)
    if (multiSelectionMode) {
      rowSelection = "multiple"
      rowMultiSelectWithClick = true
      if (columnDefs.size > 0) {
        columnDefs[0].checkboxSelection = true
        columnDefs[0].headerCheckboxSelection = true
        multiSelectButtonTitle = translate("next")
      }
    }
    return this
  }

  fun withPinnedLeft(col: Int) {
    columnDefs.forEachIndexed { index, columnDef ->
      if (index < col) {
        columnDef.withPinnedLeft()
      }
    }
  }
}
