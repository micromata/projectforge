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

import org.projectforge.business.configuration.ConfigurationServiceAccessor
import org.projectforge.common.DateFormatType
import org.projectforge.common.props.PropertyType
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateFormats
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
  var type: String? = null,
  var checkboxSelection: Boolean? = null,
  var headerCheckboxSelection: Boolean? = null,
  var width: Int? = null,
  var minWidth: Int? = null,
  var maxWidth: Int? = null,
  var resizable: Boolean? = true,
) {
  var pinned: String? = null

  /**
   * https://www.ag-grid.com/react-data-grid/components/
   * If formatter is used, it's set to "formatter".
   */
  var cellRenderer: String? = null

  var cellRendererParams: Map<String, Any>? = null

  /**
   * https://www.ag-grid.com/react-data-grid/value-formatters/
   * Not yet implemented.
   */
  //var valueFormatter: String? = null

  /**
   * https://www.ag-grid.com/react-data-grid/column-definitions/#right-aligned-and-numeric-columns
   */
  enum class AG_TYPE(val agType: String) { NUMERIC_COLUMN("numericColumn"), RIGHT_ALIGNED("rightAligned") }

  enum class Formatter {
    BOOLEAN,
    CURRENCY,
    DATE,
    NUMBER,
    TIMESTAMP_MINUTES,
    TIMESTAMP_SECONDS,
    RATING,

    ADDRESS_BOOK,
    AUFTRAG_POSITION,
    EMPLOYEE,
    COST1,
    COST2,
    CUSTOMER,
    GROUP,
    KONTO,
    PROJECT,
    TASK_PATH,
    USER,
  }

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
    ): UIAgGridColumnDef {
      return createCol(
        null,
        field = field,
        sortable = sortable,
        width = width,
        headerName = headerName,
        valueGetter = valueGetter,
        formatter = valueFormatter,
      )
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
      formatter: Formatter? = null,
      lcField: String = field,
    ): UIAgGridColumnDef {
      val col = UIAgGridColumnDef(field, sortable = sortable)
      lc?.idPrefix?.let {
        col.field = "${it}${col.field}"
      }
      if (headerName != null) {
        col.headerName = headerName
      }
      val elementInfo = ElementsRegistry.getElementInfo(lc, lcField)
      var useFormatter = formatter
      if (elementInfo != null) {
        if (col.headerName == null) {
          col.headerName = elementInfo.i18nKey
        }
        /* col.dataType = UIDataTypeUtils.ensureDataType(elementInfo)
       if (col.dataType == UIDataType.BOOLEAN) {
         col.setStandardBoolean()
       }*/
        if (useFormatter == null) {
          // Try to determine formatter by type and propertyInfo (defined on DO-field):
          if (LocalDate::class.java == elementInfo.propertyClass) {
            useFormatter = Formatter.DATE
          } else if (Number::class.java.isAssignableFrom(elementInfo.propertyClass)) {
            if (elementInfo.propertyType == PropertyType.CURRENCY) {
              useFormatter = Formatter.CURRENCY
            } else {
              useFormatter = Formatter.NUMBER
            }
          } else if (elementInfo.propertyClass == LocalDate::class.java) {
            useFormatter = Formatter.DATE
          } else if (elementInfo.propertyClass == PFUserDO::class.java) {
            useFormatter = Formatter.USER
          } else if (java.util.Date::class.java == elementInfo.propertyClass) {
            if (field in arrayOf("created", "lastUpdate")) {
              useFormatter = Formatter.DATE
            } else {
              useFormatter = Formatter.TIMESTAMP_MINUTES
            }
          } else if (elementInfo.propertyClass == Boolean::class.java || elementInfo.propertyClass == java.lang.Boolean::class.java) {
            useFormatter = Formatter.BOOLEAN
          }
        }
        if (useFormatter == null) {
          if (valueGetter.isNullOrBlank() && DisplayNameCapable::class.java.isAssignableFrom(elementInfo.propertyClass)) {
            col.valueGetter = "data?.${col.field}?.displayName"
          }
        }
      }
      width?.let { col.width = it }
      useFormatter?.let {
        when (it) {
          Formatter.CURRENCY -> {
            if (width == null) {
              col.width = 120
              col.type = AG_TYPE.NUMERIC_COLUMN.agType
            }
          }
          Formatter.DATE -> {
            col.width = 100
          }
          else -> {}
        }
      }
      valueGetter?.let { col.valueGetter = it }
      useFormatter?.let {
        col.cellRenderer = "formatter"
        col.cellRendererParams = createCellRendererParams(it)
      }
      return col
    }

    private fun createCellRendererParams(formatter: Formatter): Map<String, Any> {
      val result = mutableMapOf<String, Any>("dataType" to formatter.name)
      when (formatter) {
        Formatter.DATE -> result["dateFormat"] = DateFormats.getFormatString(DateFormatType.DATE)
        Formatter.TIMESTAMP_MINUTES -> result["timestampFormatMinutes"] = DateFormats.getFormatString(DateFormatType.DATE_TIME_MINUTES)
        Formatter.TIMESTAMP_SECONDS -> result["timestampFormatSeconds"] = DateFormats.getFormatString(DateFormatType.DATE_TIME_SECONDS)
        Formatter.CURRENCY -> {
          result["locale"] = ThreadLocalUserContext.getLocale()
          result["currency"] = ConfigurationServiceAccessor.get().currency ?: "EUR"
        }
        else -> {}
      }
      return result
    }
  }
}
