/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

data class UITableColumn(var id: String,
                         var title: String? = null,
                         var titleIcon: UIIconType? = null,
                         var tooltip: String? = null,
                         var dataType: UIDataType = UIDataType.STRING,
                         var sortable: Boolean = true,
                         var formatter: Formatter? = null,
                         var valueIconMap: Map<Any, UIIconType?>? = null)
    : UIElement(UIElementType.TABLE_COLUMN) {

    /**
     * Helper method for setting properties. Null values are ignored.
     * @return this for chaining.
     */
    fun set(title: String? = null, tooltip: String? = null, dataType: UIDataType? = null, sortable: Boolean? = null, formatter: Formatter? = null): UITableColumn {
        title?.let { this.title = it }
        tooltip?.let { this.tooltip = it }
        dataType?.let { this.dataType = it }
        sortable?.let { this.sortable = it }
        formatter?.let { this.formatter = it }
        return this
    }

    /**
     * @return this for chaining.
     */
    fun setStandardBoolean(): UITableColumn {
        valueIconMap = mapOf(true to UIIconType.CHECKED, false to null)
        dataType = UIDataType.BOOLEAN
        return this
    }
}

enum class Formatter {
    ADDRESS_BOOK,
    AUFTRAG_POSITION,
    DATE,
    EMPLOYEE,
    COST1,
    COST2,
    CUSTOMER,
    GROUP,
    KONTO,
    PROJECT,
    RATING,
    TASK_PATH,
    TIMESTAMP_MINUTES,
    USER
}
