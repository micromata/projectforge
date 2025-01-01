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

open class UITable(
    val id: String,
    val columns: MutableList<UITableColumn> = mutableListOf(),
    val listPageTable: Boolean = false,
    var rowClickPostUrl: String? = null,
    /**
     * If given, the entries of the table will be refreshed by calling this post url. This works only, if the
     * entries of the table are given as variables (see LogViewer as a reference).
     */
    var refreshUrl: String? = null,
    /**
     * If refreshUrl is given, method is the HTTP method to call the refreshUrl ("POST" and "GET" is supported).
     */
    var refreshMethod: RefreshMethod = RefreshMethod.POST,
    /**
     * The given refreshUrl (if any) will be called every refreshIntervalSeconds seconds.
     */
    var refreshIntervalSeconds: Int? = null,
    /**
     * If given, the React component calls refresh only, if the auto-refresh flag of the data model is true. This is
     * the name of the flag property (see LogViewer as a reference).
     */
    var autoRefreshFlag: String? = null,
) : UIElement(if (listPageTable) UIElementType.TABLE_LIST_PAGE else UIElementType.TABLE) {
    enum class RefreshMethod { POST, GET, SSE }

    companion object {
        @JvmStatic
        fun createUIResultSetTable(): UITable {
            return UITable("resultSet", listPageTable = true)
        }
    }

    fun add(column: UITableColumn): UITable {
        columns.add(column)
        return this
    }

    /**
     * For adding columns with the given ids
     */
    fun add(lc: LayoutContext, vararg columnIds: String, sortable: Boolean = true): UITable {
        columnIds.forEach {
            val col = UITableColumn(it, sortable = sortable)
            val elementInfo = ElementsRegistry.getElementInfo(lc, it)
            if (elementInfo != null) {
                col.title = elementInfo.i18nKey
                col.dataType = UIDataTypeUtils.ensureDataType(elementInfo)
                if (col.dataType == UIDataType.BOOLEAN) {
                    col.setStandardBoolean()
                }
            }
            if (!lc.idPrefix.isNullOrBlank())
                col.id = "${lc.idPrefix}${col.id}"
            add(col)
        }
        return this
    }
}
