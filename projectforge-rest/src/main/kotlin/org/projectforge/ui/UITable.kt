/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

data class UITable(val id : String, val columns : MutableList<UITableColumn> = mutableListOf()) : UIElement(UIElementType.TABLE) {
    companion object {
       fun UIResultSetTable() : UITable {
           return UITable("resultSet")
       }
    }

    fun add(column: UITableColumn): UITable {
        columns.add(column)
        return this
    }

    /**
     * For adding columns with the given ids
     */
    fun add(layoutSettings: LayoutContext, vararg columnIds: String): UITable {
        columnIds.forEach {
            val col = UITableColumn(it)
            col.protectTitle = true
            val elementInfo = ElementsRegistry.getElementInfo(layoutSettings.dataObjectClazz, it)
            if (elementInfo != null) {
                col.title = elementInfo.i18nKey
                col.dataType = UIDataTypeUtils.getDataType(elementInfo)
            }
            if (!layoutSettings.idPrefix.isNullOrBlank())
                col.id = "${layoutSettings.idPrefix}${col.id}"
            add(col)
        }
        return this
    }
}
