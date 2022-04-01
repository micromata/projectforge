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
import java.time.LocalDate

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
  var resizable: Boolean? = true,
) {
  var pinned: String? = null

  enum class AG_TYPE(val agType: String) { NUMERIC_COLUMN("numericColumn"), RIGHT_ALIGNED("rightAligned") }

  enum class PF_STYLE { CURRENCY, LOCALE_DATE }

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
    /**
     * @param lcField If field name of dto differs from do (e. g. kost2.project vs. kost2.projekt)
     */
    fun createCol(
      field: String,
      sortable: Boolean = true,
      width: Int? = null,
      headerName: String? = null,
      valueGetter: String? = null,
      valueFormatter: Formatter? = null,
      pfStyle: PF_STYLE? = null,
    ): UIAgGridColumnDef {
      return createCol(null, field = field, sortable = sortable, width = width, headerName = headerName, valueGetter = valueGetter, valueFormatter = valueFormatter, pfStyle = pfStyle)
    }

    /**
     * @param lcField If field name of dto differs from do (e. g. kost2.project vs. kost2.projekt)
     */
    fun createCol(
      lc: LayoutContext?,
      field: String,
      sortable: Boolean = true,
      width: Int? = null,
      headerName: String? = null,
      valueGetter: String? = null,
      valueFormatter: Formatter? = null,
      lcField: String = field,
      pfStyle: PF_STYLE? = null,
    ): UIAgGridColumnDef {
      val col = UIAgGridColumnDef(field, sortable = sortable)
      lc?.idPrefix?.let {
        col.field = "${it}${col.field}"
      }
      if (headerName != null) {
        col.headerName = headerName
      }
      val elementInfo = ElementsRegistry.getElementInfo(lc, lcField)
      var myStyle = pfStyle
      if (elementInfo != null) {
        if (col.headerName == null) {
          col.headerName = elementInfo.i18nKey
        }
        /* col.dataType = UIDataTypeUtils.ensureDataType(elementInfo)
       if (col.dataType == UIDataType.BOOLEAN) {
         col.setStandardBoolean()
       }*/
        if (myStyle == null) {
          if (LocalDate::class.java.isAssignableFrom(elementInfo.propertyType)) {
            myStyle = PF_STYLE.LOCALE_DATE
          }
        }
        if (valueGetter.isNullOrBlank() && DisplayNameCapable::class.java.isAssignableFrom(elementInfo.propertyType)) {
          col.valueGetter = "data.${col.field}.displayName"
        }
      }
      width?.let { col.width = it }
      myStyle?.let {
        when (it) {
          PF_STYLE.CURRENCY -> {
            if (width == null) {
              col.width = 120
              col.type = AG_TYPE.NUMERIC_COLUMN.agType
            }
          }
          PF_STYLE.LOCALE_DATE -> {
            col.width = 100
          }
        }
      }
      valueGetter?.let { col.valueGetter = it }
      col.valueFormatter = valueFormatter
      return col
    }
  }
}
