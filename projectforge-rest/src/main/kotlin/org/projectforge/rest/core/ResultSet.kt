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

package org.projectforge.rest.core

import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import java.io.Serializable

/**
 * Contains the data including the result list (matching the filter) served by getList methods ([getInitialList] and [getList]).
 * @param origResultSet Should be given, if ResultSet is converted to a new one (preselected ids will be preserved). Might be null,
 * if a new one should be created.
 */
class ResultSet<O : Any>(
    var resultSet: List<O>,
    origResultSet: ResultSet<*>?,
    var totalSize: Int? = null,
    var highlightRowId: Long? = null,
    var selectedEntityIds: Collection<Serializable>? = null,
    magicFilter: MagicFilter, // only needed to check if the result set was truncated (has size of magicFilter.maxRows).
) {
    /**
     * Result info as mark down to display. Is usable for statistics as well as for important note, that the
     * result set was runcated due to maxRows limitation.
     */
    var resultInfo: String? = null
        internal set

    val size = resultSet.size

    var paginationPageSize = magicFilter.paginationPageSize

    init {
        if (origResultSet != null && selectedEntityIds == null) {
            selectedEntityIds = origResultSet.selectedEntityIds
        }
        if (resultSet.size == magicFilter.maxRows) {
            val msg = translateMsg("search.maxRowsExceeded", magicFilter.maxRows)
            resultInfo = "<span style=\"color:red; font-weight: bold;\">$msg</span>"
        }
    }

    fun addResultInfo(info: String?) {
        if (info.isNullOrBlank()) {
            return
        }
        resultInfo.let { value ->
            if (resultInfo.isNullOrBlank()) {
                resultInfo = info
            } else {
                resultInfo = "$value\n\n$info"
            }
        }
    }

}
