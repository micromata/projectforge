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

import org.projectforge.framework.DisplayNameCapable

/**
 * Column def AgGrid
 */
open class UIAgGridColumnDef(
  var field: String,
  var headerName: String? = null,
  var sortable: Boolean = false,
  var filter: Boolean = false,
  var valueGetter: String? = null,
  var valueFormatter: Formatter? = null,
  var dataType: UIDataType = UIDataType.STRING,
  var type: String? = null,
  var checkboxSelection: Boolean? = null,
  var headerCheckboxSelection: Boolean? = null,
  var width: Int? = null,
  var minWidth: Int? = null,
  var maxWidth: Int? = null,
) {
  var pinned: String? = null

  enum class AG_TYPE(val agType: String) { NUMERIC_COLUMN("numericColumn"), RIGHT_ALIGNED("rightAligned") }

  fun withAGType(type: AG_TYPE): UIAgGridColumnDef {
    this.type = type.agType
    return this
  }

  fun withPinnedLeft(): UIAgGridColumnDef {
    pinned = "left"
    return this
  }

  fun withPinnedRight(): UIAgGridColumnDef {
    pinned = "right"
    return this
  }

  companion object {
    fun createCurrencyCol(
      lc: LayoutContext, field: String,
      sortable: Boolean = false,
      width: Int = 120,
      headerName: String? = null
    ): UIAgGridColumnDef {
      return createCol(
        lc,
        field,
        sortable = sortable,
        width = width,
        headerName = headerName
      ).withAGType(AG_TYPE.NUMERIC_COLUMN)
    }

    /**
     * @param lcField If field name of dto differs from do (e. g. kost2.project vs. kost2.projekt)
     */
    fun createCol(
      lc: LayoutContext,
      field: String,
      sortable: Boolean = true,
      width: Int? = null,
      headerName: String? = null,
      valueGetter: String? = null,
      valueFormatter: Formatter? = null,
      lcField: String = field,
    ): UIAgGridColumnDef {
      val col = UIAgGridColumnDef(field, sortable = sortable)
      if (!lc.idPrefix.isNullOrBlank())
        col.field = "${lc.idPrefix}${col.field}"
      if (headerName != null) {
        col.headerName = headerName
      }
      val elementInfo = ElementsRegistry.getElementInfo(lc, lcField)
      if (elementInfo != null) {
        if (col.headerName == null) {
          col.headerName = elementInfo.i18nKey
        }
        /* col.dataType = UIDataTypeUtils.ensureDataType(elementInfo)
       if (col.dataType == UIDataType.BOOLEAN) {
         col.setStandardBoolean()
       }*/
        if (valueGetter.isNullOrBlank() && DisplayNameCapable::class.java.isAssignableFrom(elementInfo.propertyType)) {
          col.valueGetter = "data.${col.field}.displayName"
        }
      }
      width?.let { col.width = it }
      valueGetter?.let { col.valueGetter = it }
      col.valueFormatter = valueFormatter
      return col
    }
  }
}
