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

package org.projectforge.ui

import org.projectforge.business.configuration.ConfigurationServiceAccessor
import org.projectforge.common.DateFormatType
import org.projectforge.common.props.PropertyType
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateFormats
import java.time.LocalDate
import kotlin.reflect.KProperty

/**
 * Column def AgGrid
 */
open class UIAgGridColumnDef(
    var field: String? = null,
    var headerName: String? = null,
    var headerTooltip: String? = null,
    var sortable: Boolean = false,
    /**
     * If true, auto-filter is shown, if false, no auto-filter is shown. AG grid supports also string values
     * such as "agNumberColumnFilter" or "agSetColumnFilter".
     */
    var filter: Any? = true,
    var valueGetter: String? = null,
    var type: String? = null,
    var minWidth: Int? = null,
    var maxWidth: Int? = null,
    /**
     * width in Pixel.
     */
    var width: Int? = null,
    var hide: Boolean? = null,
    var resizable: Boolean? = true,
    /**
     * https://www.ag-grid.com/react-data-grid/value-formatters/
     */
    var valueFormatter: String? = null,
    /**
     * https://www.ag-grid.com/react-data-grid/components/
     * If formatter is used, it's set to "formatter".
     */
    var cellRenderer: String? = null,
    var wrapText: Boolean? = null,
    var autoHeight: Boolean? = wrapText,
) {
    enum class Type {
        NUMBER
    }

    class FilterParams {
        var buttons: Array<String>? = null
    }

    var pinned: String? = null

    var cellRendererParams: Map<String, Any>? = null

    /**
     * https://www.ag-grid.com/react-data-grid/column-properties/
     */
    var headerClass: Array<String>? = null

    var tooltipField: String? = null

    var suppressSizeToFit: Boolean? = null

    var filterParams: FilterParams? = null

    /**
     * https://www.ag-grid.com/react-data-grid/column-definitions/#right-aligned-and-numeric-columns
     */
    enum class AG_TYPE(val agType: String) { NUMERIC_COLUMN("numericColumn"), RIGHT_ALIGNED("rightAligned") }

    enum class Formatter {
        BOOLEAN,
        CONSUMPTION,
        CURRENCY,
        DATE,
        NUMBER,
        TIMESTAMP_MINUTES,
        TIMESTAMP_SECONDS,
        RATING,

        /**
         * The object has a field displayName, which should be displayed.
         */
        SHOW_DISPLAYNAME,

        /**
         * Each object of the list has a field displayName, which should be displayed comma separated.
         */
        SHOW_LIST_OF_DISPLAYNAMES,

        /**
         * Tree view (task tree).
         */
        TREE_NAVIGATION,

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

    fun withSuppressSizeToFit(): UIAgGridColumnDef {
        suppressSizeToFit = true
        return this
    }

    fun withTooltipField(tooltipField: String): UIAgGridColumnDef {
        this.tooltipField = tooltipField
        return this
    }

    fun setApplyAndResetButton(): FilterParams {
        filterParams = filterParams ?: FilterParams()
        return filterParams!!.also {
            it.buttons = arrayOf("apply", "reset")
        }
    }

    /**
     * Sets the formatter. Also sets cellRenderer to "formatter".
     * @param formatter The formatter to set.
     * @return this for chaining.
     */
    fun setFormat(formatter: Formatter): UIAgGridColumnDef {
        when (formatter) {
            Formatter.CURRENCY -> {
                if (width == null) {
                    this.width = CURRENCY_WIDTH
                }
                this.type = AG_TYPE.NUMERIC_COLUMN.agType
                this.filter = "agNumberColumnFilter"
            }

            Formatter.NUMBER -> {
                if (width == null) {
                    this.width = NUMBER_WIDTH
                }
                this.type = AG_TYPE.NUMERIC_COLUMN.agType
                this.filter = "agNumberColumnFilter"
                this.setApplyAndResetButton()
            }

            Formatter.CONSUMPTION -> {
                if (width == null) {
                    this.width = 80
                }
            }

            else -> {
            }
        }
        cellRenderer = "formatter"
        cellRendererParams = createCellRendererParams(formatter)
        return this
    }

    companion object {
        fun createCol(
            property: KProperty<*>,
            sortable: Boolean = true,
            width: Int? = null,
            headerName: String? = null,
            headerTooltip: String? = null,
            valueGetter: String? = null,
            valueFormatter: Formatter? = null,
            wrapText: Boolean? = null,
            autoHeight: Boolean? = wrapText,
            valueIconMap: Map<Any, UIIconType?>? = null,
            tooltipField: String? = null,
        ): UIAgGridColumnDef {
            return createCol(
                null,
                field = property.name,
                sortable = sortable,
                width = width,
                headerName = headerName,
                headerTooltip = headerTooltip,
                valueGetter = valueGetter,
                formatter = valueFormatter,
                wrapText = wrapText,
                autoHeight = autoHeight,
                valueIconMap = valueIconMap,
                tooltipField = tooltipField,
            )
        }

        /**
         * @param width Column width in pixel.
         */
        fun createCol(
            field: String,
            sortable: Boolean = true,
            width: Int? = null,
            headerName: String? = null,
            headerTooltip: String? = null,
            valueGetter: String? = null,
            valueFormatter: Formatter? = null,
            wrapText: Boolean? = null,
            autoHeight: Boolean? = wrapText,
            valueIconMap: Map<Any, UIIconType?>? = null,
            tooltipField: String? = null,
            filter: Any? = null,
        ): UIAgGridColumnDef {
            return createCol(
                null,
                field = field,
                sortable = sortable,
                width = width,
                headerName = headerName,
                headerTooltip = headerTooltip,
                valueGetter = valueGetter,
                formatter = valueFormatter,
                wrapText = wrapText,
                autoHeight = autoHeight,
                valueIconMap = valueIconMap,
                tooltipField = tooltipField,
                filter = filter,
            )
        }

        /**
         * @param lcField If field name of dto differs from do (e.g. kost2.project vs. kost2.projekt)
         * @param width Column width in pixel.
         */
        fun createCol(
            lc: LayoutContext?,
            property: KProperty<*>,
            sortable: Boolean = true,
            width: Int? = null,
            headerName: String? = null,
            headerTooltip: String? = null,
            valueGetter: String? = null,
            formatter: Formatter? = null,
            lcField: String = property.name,
            wrapText: Boolean? = null,
            autoHeight: Boolean? = wrapText,
            valueIconMap: Map<Any, UIIconType?>? = null,
            tooltipField: String? = null,
        ): UIAgGridColumnDef {
            return createCol(
                lc,
                field = property.name,
                sortable = sortable,
                width = width,
                headerName = headerName,
                headerTooltip = headerTooltip,
                valueGetter = valueGetter,
                formatter = formatter,
                lcField = lcField,
                wrapText = wrapText,
                autoHeight = autoHeight,
                valueIconMap = valueIconMap,
                tooltipField = tooltipField,
            )
        }

        /**
         * @param lcField If field name of dto differs from do (e. g. kost2.project vs. kost2.projekt)
         * @param width Column width in pixel.
         */
        fun createCol(
            lc: LayoutContext?,
            field: String,
            sortable: Boolean = true,
            width: Int? = null,
            headerName: String? = null,
            headerTooltip: String? = null,
            valueGetter: String? = null,
            formatter: Formatter? = null,
            lcField: String = field,
            wrapText: Boolean? = null,
            autoHeight: Boolean? = wrapText,
            valueIconMap: Map<Any, UIIconType?>? = null,
            tooltipField: String? = null,
            type: Type? = null,
            filter: Any? = null,
        ): UIAgGridColumnDef {
            val col = UIAgGridColumnDef(
                field,
                sortable = sortable,
                wrapText = wrapText,
                autoHeight = autoHeight,
                filter = filter,
            )
            lc?.idPrefix?.let {
                col.field = "${it}${col.field}"
            }
            if (headerName != null) {
                col.headerName = headerName
            }
            if (headerTooltip != null) {
                col.headerTooltip = headerTooltip
            }
            if (tooltipField != null) {
                col.tooltipField = tooltipField
            }
            val elementInfo = ElementsRegistry.getElementInfo(lc, lcField)
            var useFormatter = formatter
            if (elementInfo != null) {
                if (col.headerName == null) {
                    col.headerName = translate(elementInfo.i18nKey)
                }
                if (type == null && useFormatter == null) {
                    // Try to determine formatter by type and propertyInfo (defined on DO-field):
                    if (Number::class.java.isAssignableFrom(elementInfo.propertyClass)) {
                        if (elementInfo.propertyType == PropertyType.CURRENCY) {
                            useFormatter = Formatter.CURRENCY
                        } else {
                            useFormatter = Formatter.NUMBER
                        }
                    } else if (elementInfo.propertyClass == LocalDate::class.java) {
                        if (width == null) {
                            col.width = DATE_WIDTH
                        }
                        col.filter = "agDateColumnFilter"
                        col.setApplyAndResetButton()
                    } else if (java.util.Date::class.java == elementInfo.propertyClass) {
                        if (field in arrayOf("created", "lastUpdate")) {
                            useFormatter = Formatter.DATE
                        } else {
                            useFormatter = Formatter.TIMESTAMP_MINUTES
                        }
                    } else if (elementInfo.propertyClass == String::class.java) {
                        if ((elementInfo.maxLength ?: 0) > 1000 && width == null) {
                            col.width = LONG_DESCRIPTION_WIDTH // Extra wide column
                        }
                    } else if (elementInfo.propertyClass == Boolean::class.java || elementInfo.propertyClass == java.lang.Boolean::class.java) {
                        useFormatter = Formatter.BOOLEAN
                    }
                }
                if (useFormatter == null) {
                    if (valueGetter.isNullOrBlank() && DisplayNameCapable::class.java.isAssignableFrom(elementInfo.propertyClass)) {
                        col.valueGetter = "data?.${col.field}?.displayName"
                    } else if (field == "attachmentsSizeFormatted") {
                        col.headerClass = arrayOf("icon", "icon-solid", "icon-paperclip")
                        col.headerName = ""
                        col.headerTooltip = translate("attachments")
                        col.width = 30
                    }
                }
                if (col.valueGetter.isNullOrBlank()
                    && valueGetter.isNullOrBlank()
                    && DisplayNameCapable::class.java.isAssignableFrom(elementInfo.propertyClass)
                ) {
                    col.valueGetter = "data?.${col.field}?.displayName"
                }
            }
            if (type != null) {
                when (type) {
                    Type.NUMBER -> {
                        if (width == null) {
                            col.width = NUMBER_WIDTH
                        }
                        col.type = AG_TYPE.NUMERIC_COLUMN.agType
                        col.filter = "agNumberColumnFilter"
                        col.setApplyAndResetButton()
                    }
                }
            }
            if (width != null) {
                col.width = width
            }
            useFormatter?.let {
                col.setFormat(it)
            }
            if (useFormatter == null
                && elementInfo?.propertyType?.isIn(PropertyType.INPUT, PropertyType.UNSPECIFIED) == true
                && (elementInfo.maxLength ?: 0) > 256
            ) {
                // Use text filter for long texts.
                col.filter = "agTextColumnFilter"
                col.setApplyAndResetButton()
            }
            valueGetter?.let { col.valueGetter = it }
            if (!valueIconMap.isNullOrEmpty()) {
                if (col.cellRenderer.isNullOrEmpty()) {
                    col.cellRenderer = "formatter"
                }
                col.cellRendererParams = (col.cellRendererParams?.toMutableMap() ?: mutableMapOf()).apply {
                    this["valueIconMap"] = valueIconMap
                }
            }
            return col
        }

        private fun createCellRendererParams(formatter: Formatter): MutableMap<String, Any> {
            val result = mutableMapOf<String, Any>("dataType" to formatter.name)
            when (formatter) {
                Formatter.DATE -> result["dateFormat"] = DateFormats.getFormatString(DateFormatType.DATE)
                Formatter.TIMESTAMP_MINUTES -> result["timestampFormatMinutes"] =
                    DateFormats.getFormatString(DateFormatType.DATE_TIME_MINUTES)

                Formatter.TIMESTAMP_SECONDS -> result["timestampFormatSeconds"] =
                    DateFormats.getFormatString(DateFormatType.DATE_TIME_SECONDS)

                Formatter.CURRENCY -> {
                    result["locale"] = ThreadLocalUserContext.localeAsString
                    result["currency"] = ConfigurationServiceAccessor.get().currency ?: "EUR"
                }

                else -> {}
            }
            return result
        }

        const val CURRENCY_WIDTH = 120
        const val DATE_WIDTH = 100
        const val DESCRIPTION_WIDTH = 300
        const val LONG_DESCRIPTION_WIDTH = 500
        const val NUMBER_WIDTH = 100
        const val TIMESTAMP_WIDTH = 120
        const val USER_WIDTH = 100
    }
}
